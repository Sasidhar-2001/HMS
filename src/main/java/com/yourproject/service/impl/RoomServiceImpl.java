package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.*;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.OccupancyRepository;
import com.yourproject.repository.RoomRepository;
import com.yourproject.repository.UserRepository;
import com.yourproject.service.RoomService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final OccupancyRepository occupancyRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository,
                           UserRepository userRepository,
                           OccupancyRepository occupancyRepository,
                           ModelMapper modelMapper) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.occupancyRepository = occupancyRepository;
        this.modelMapper = modelMapper;
    }

    private RoomDto convertToDto(Room room) {
        RoomDto roomDto = modelMapper.map(room, RoomDto.class);
        roomDto.setCurrentOccupancyCount(
            (int) room.getOccupancies().stream().filter(Occupancy::isActive).count()
        );
        roomDto.setAvailable(room.getStatus() == RoomStatus.AVAILABLE && roomDto.getCurrentOccupancyCount() < room.getCapacity());

        List<OccupancyDto> occupancyDtos = room.getOccupancies().stream()
            .filter(Occupancy::isActive) // Or all, depending on what RoomDto needs
            .map(occ -> {
                OccupancyDto occDto = modelMapper.map(occ, OccupancyDto.class);
                if (occ.getStudent() != null) {
                    occDto.setStudent(modelMapper.map(occ.getStudent(), UserSlimDto.class));
                }
                return occDto;
            })
            .collect(Collectors.toList());
        roomDto.setCurrentOccupancies(occupancyDtos);

        // Map maintenance history if needed (assuming MaintenanceRecordDto exists)
        if (room.getMaintenanceHistory() != null) {
            roomDto.setMaintenanceHistory(room.getMaintenanceHistory().stream()
                .map(mh -> modelMapper.map(mh, MaintenanceRecordDto.class))
                .collect(Collectors.toList()));
        }
        return roomDto;
    }


    @Override
    @Transactional
    public RoomDto createRoom(RoomRequestDto roomRequestDto) {
        if (roomRepository.findByRoomNumber(roomRequestDto.getRoomNumber()).isPresent()) {
            throw new BadRequestException("Room with number " + roomRequestDto.getRoomNumber() + " already exists.");
        }
        Room room = modelMapper.map(roomRequestDto, Room.class);
        if (roomRequestDto.getStatus() == null) {
            room.setStatus(RoomStatus.AVAILABLE); // Default status
        }
        room.setActive(true);
        Room savedRoom = roomRepository.save(room);
        return convertToDto(savedRoom);
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        return convertToDto(findRoomEntityById(roomId));
    }

    @Override
    public RoomDto getRoomByNumber(String roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with number: " + roomNumber));
        return convertToDto(room);
    }

    @Override
    public Page<RoomDto> getAllRooms(Pageable pageable, String blockFilter, Integer floorFilter, String typeFilter, String statusFilter) {
        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive"))); // Always filter active rooms by default

            if (StringUtils.hasText(blockFilter)) {
                predicates.add(cb.equal(root.get("block"), blockFilter));
            }
            if (floorFilter != null) {
                predicates.add(cb.equal(root.get("floor"), floorFilter));
            }
            if (StringUtils.hasText(typeFilter)) {
                try {
                    RoomType type = RoomType.valueOf(typeFilter.toUpperCase());
                    predicates.add(cb.equal(root.get("type"), type));
                } catch (IllegalArgumentException e) { /* ignore invalid type */ }
            }
            if (StringUtils.hasText(statusFilter)) {
                 try {
                    RoomStatus status = RoomStatus.valueOf(statusFilter.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) { /* ignore invalid status */ }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return roomRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    @Override
    public List<RoomDto> getAvailableRooms(String typeFilter, String blockFilter) {
        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.equal(root.get("status"), RoomStatus.AVAILABLE));
            // This condition is tricky with JPA spec: $expr: { $lt: ['$currentOccupancy', '$capacity'] }
            // For simplicity, we filter further in Java or use a @Query.
            // Here, we'll rely on the transient isAvailable() or refine spec if possible.
            // A direct way with spec: cb.lessThan(root.get("occupancies").size(), root.get("capacity")) - but this needs to count *active* occupancies.

            if (StringUtils.hasText(typeFilter)) {
                 try {
                    RoomType type = RoomType.valueOf(typeFilter.toUpperCase());
                    predicates.add(cb.equal(root.get("type"), type));
                } catch (IllegalArgumentException e) { /* ignore invalid type */ }
            }
            if (StringUtils.hasText(blockFilter)) {
                predicates.add(cb.equal(root.get("block"), blockFilter));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Post-filter for true availability based on active occupancy count
        return roomRepository.findAll(spec).stream()
            .filter(room -> room.getOccupancies().stream().filter(Occupancy::isActive).count() < room.getCapacity())
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomDto updateRoom(Long roomId, RoomRequestDto roomRequestDto) {
        Room room = findRoomEntityById(roomId);

        // Check if room number is being changed and if it conflicts
        if (StringUtils.hasText(roomRequestDto.getRoomNumber()) && !room.getRoomNumber().equals(roomRequestDto.getRoomNumber())) {
            if(roomRepository.findByRoomNumber(roomRequestDto.getRoomNumber()).filter(r -> !r.getId().equals(roomId)).isPresent()){
                 throw new BadRequestException("Room with number " + roomRequestDto.getRoomNumber() + " already exists.");
            }
             room.setRoomNumber(roomRequestDto.getRoomNumber());
        }

        if(roomRequestDto.getFloor() != null) room.setFloor(roomRequestDto.getFloor());
        if(StringUtils.hasText(roomRequestDto.getBlock())) room.setBlock(roomRequestDto.getBlock());
        if(roomRequestDto.getType() != null) room.setType(roomRequestDto.getType());
        if(roomRequestDto.getCapacity() != null) room.setCapacity(roomRequestDto.getCapacity());
        if(roomRequestDto.getAmenities() != null) room.setAmenities(roomRequestDto.getAmenities());
        if(roomRequestDto.getMonthlyRent() != null) room.setMonthlyRent(roomRequestDto.getMonthlyRent());
        if(roomRequestDto.getSecurityDeposit() != null) room.setSecurityDeposit(roomRequestDto.getSecurityDeposit());
        if(roomRequestDto.getStatus() != null) room.setStatus(roomRequestDto.getStatus());
        if(roomRequestDto.getImages() != null) room.setImages(roomRequestDto.getImages());
        if(StringUtils.hasText(roomRequestDto.getDescription())) room.setDescription(roomRequestDto.getDescription());
        if(roomRequestDto.getIsActive() != null) room.setActive(roomRequestDto.getIsActive());

        // Logic for updating status based on occupancy if capacity changes
        // This is complex and should be carefully handled, possibly after save and re-fetch.
        // For now, direct status update is allowed.

        Room updatedRoom = roomRepository.save(room);
        return convertToDto(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) { // Deactivation
        Room room = findRoomEntityById(roomId);
        if (room.getOccupancies().stream().anyMatch(Occupancy::isActive)) {
            throw new BadRequestException("Cannot deactivate room with active occupants.");
        }
        room.setActive(false);
        // Optionally change status to something like 'DECOMMISSIONED'
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public OccupancyDto assignStudentToRoom(Long studentId, Long roomId, Integer bedNumber) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        Room room = findRoomEntityById(roomId);

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can be assigned to rooms.");
        }

        // Check if student already has an active occupancy
        if (occupancyRepository.existsByStudentAndIsActiveTrue(student)) {
             throw new BadRequestException("Student " + student.getFirstName() + " is already assigned to a room.");
        }

        long activeOccupantsInRoom = occupancyRepository.countByRoomAndIsActiveTrue(room);
        if (activeOccupantsInRoom >= room.getCapacity()) {
            throw new BadRequestException("Room " + room.getRoomNumber() + " is at full capacity.");
        }
        if (room.getStatus() != RoomStatus.AVAILABLE && room.getStatus() != RoomStatus.OCCUPIED) { // Allow assigning to partially occupied
             throw new BadRequestException("Room " + room.getRoomNumber() + " is not available for assignment (current status: "+room.getStatus()+").");
        }


        Occupancy newOccupancy = new Occupancy();
        newOccupancy.setStudent(student);
        newOccupancy.setRoom(room);
        newOccupancy.setAllocatedDate(LocalDate.now());
        newOccupancy.setActive(true);
        if (bedNumber != null) {
            newOccupancy.setBedNumber(bedNumber);
        } else {
            // Basic auto-assignment of bed number (can be more complex)
            newOccupancy.setBedNumber((int) activeOccupantsInRoom + 1);
        }

        Occupancy savedOccupancy = occupancyRepository.save(newOccupancy);

        // Update room status if it becomes full
        if (activeOccupantsInRoom + 1 >= room.getCapacity()) {
            room.setStatus(RoomStatus.OCCUPIED);
        } else {
            room.setStatus(RoomStatus.OCCUPIED); // Or keep AVAILABLE if partially filled is considered available for more
        }
        roomRepository.save(room);

        OccupancyDto occDto = modelMapper.map(savedOccupancy, OccupancyDto.class);
        occDto.setStudent(modelMapper.map(student, UserSlimDto.class));
        // occDto.setRoom(modelMapper.map(room, RoomSlimDto.class)); // Not usually needed here
        return occDto;
    }

    @Override
    @Transactional
    public void removeStudentFromRoom(Long studentId, Long roomId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        Room room = findRoomEntityById(roomId); // To ensure room context, though occupancy might be enough

        Occupancy currentOccupancy = occupancyRepository.findByStudentAndIsActiveTrue(student)
            .orElseThrow(() -> new BadRequestException("Student is not actively assigned to any room."));

        if (!currentOccupancy.getRoom().getId().equals(roomId)) {
            throw new BadRequestException("Student is not assigned to the specified room.");
        }

        currentOccupancy.setActive(false);
        currentOccupancy.setVacatedDate(LocalDate.now());
        occupancyRepository.save(currentOccupancy);

        // Update room status if it becomes available
        long activeOccupantsInRoom = occupancyRepository.countByRoomAndIsActiveTrue(room);
        if (activeOccupantsInRoom < room.getCapacity() && room.getStatus() == RoomStatus.OCCUPIED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
    }

    // @Override
    // public RoomStatsDto getRoomStats() {
    //     // Implementation for stats (counts by type, status, occupancy rate etc.)
    //     return new RoomStatsDto();
    // }

    @Override
    public Room findRoomEntityById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
    }
}
