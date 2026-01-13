package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DestinationService {

    @Autowired
    private DestinationRepository destinationRepository;

    public Destination createDestination(Destination destination) {
        if (destination.getDestinationName() == null || destination.getDestinationName().trim().isEmpty()) {
            throw new BadRequestException("Tên điểm đến không thể để trống!");
        }
        return destinationRepository.save(destination);
    }

    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    public Optional<Destination> getDestinationById(Long id) {
        return destinationRepository.findById(id);
    }

    public Destination updateDestination(Long id, Destination destinationDetails) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm đến này!"));

        if (destinationDetails.getDestinationName() != null && !destinationDetails.getDestinationName().trim().isEmpty()) {
            destination.setDestinationName(destinationDetails.getDestinationName());
        }

        return destinationRepository.save(destination);
    }

    public void deleteDestination(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm đến này!"));
        destinationRepository.delete(destination);
    }

}