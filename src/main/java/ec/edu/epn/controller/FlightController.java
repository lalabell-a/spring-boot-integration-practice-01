package ec.edu.epn.controller;

import ec.edu.epn.dto.FlightRequest;
import ec.edu.epn.model.Flight;
import ec.edu.epn.service.FlightService;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public ResponseEntity<Flight> create(@Valid @RequestBody FlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Flight>> findAll() {
        return ResponseEntity.ok(flightService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flight> findById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.findById(id));
    }

    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<Flight> findByFlightNumber(@PathVariable String flightNumber) {
        return ResponseEntity.ok(flightService.findByFlightNumber(flightNumber));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Flight>> findByStatus(@PathVariable String status) {
        return ResponseEntity.ok(flightService.findByStatus(status));
    }

@GetMapping("/dates") 
    public ResponseEntity<List<Flight>> findBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(flightService.findFlightsBetweenDates(start, end));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Flight> update(@PathVariable Long id, @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.update(id, request));
    }

    @PostMapping("/{flightId}/passengers/{passengerId}")
    public ResponseEntity<Flight> addPassenger(@PathVariable Long flightId, @PathVariable Long passengerId) {
        return ResponseEntity.ok(flightService.addPassenger(flightId, passengerId));
    }

    @DeleteMapping("/{flightId}/passengers/{passengerId}")
    public ResponseEntity<Flight> removePassenger(@PathVariable Long flightId, @PathVariable Long passengerId) {
        return ResponseEntity.ok(flightService.removePassenger(flightId, passengerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
