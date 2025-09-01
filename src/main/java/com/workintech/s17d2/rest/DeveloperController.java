package com.workintech.s17d2.rest;

import com.workintech.s17d2.model.Developer;
import com.workintech.s17d2.model.JuniorDeveloper;
import com.workintech.s17d2.model.MidDeveloper;
import com.workintech.s17d2.model.SeniorDeveloper;
import com.workintech.s17d2.tax.Taxable;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class DeveloperController {
    private final Taxable taxable;
    public Map<Integer, Developer> developers;

    public Map<Integer, Developer> getDevelopers() { return developers; }

    private double netOf(double gross, double rate) {
        return gross - (gross * rate);
    }

    // DI ile Taxable (DeveloperTax) gelir
    @Autowired
    public DeveloperController(Taxable taxable) {
        this.taxable = taxable;
    }

    @PostConstruct
    public void init() {
        developers = new ConcurrentHashMap<>();
        developers.put(101, new JuniorDeveloper(101, "Ali", netOf(50000d, taxable.getSimpleTaxRate())));
        developers.put(102, new MidDeveloper(102, "Ayşe", netOf(80000d, taxable.getMiddleTaxRate())));
        developers.put(103, new SeniorDeveloper(103, "Mehmet", netOf(120000d, taxable.getUpperTaxRate())));
    }

    @GetMapping("/developers")
    public List<Developer> findAll() {
        return new ArrayList<>(developers.values());
    }

    @GetMapping("/developers/{id}")
    public ResponseEntity<Developer> findById(@PathVariable Integer id) {
        Developer dev = developers.get(id);
        return (dev != null) ? ResponseEntity.ok(dev) : ResponseEntity.notFound().build();
    }

    @PostMapping("/developers")
    public ResponseEntity<Developer> create(@RequestBody Developer req) {
        if (req == null || req.getId() <= 0 || req.getName() == null
                || req.getSalary() <= 0 || req.getExperience() == null) {
            return ResponseEntity.badRequest().build();
        }
        /*if (developers.containsKey(req.getId())) {
            return ResponseEntity.status(409).build(); // Conflict
        }*/

        // experience'e göre doğru tip ve net salary
        Developer created = switch (req.getExperience()) {
            case JUNIOR -> new JuniorDeveloper(req.getId(), req.getName(),
                    netOf(req.getSalary(), taxable.getSimpleTaxRate()));
            case MID -> new MidDeveloper(req.getId(), req.getName(),
                    netOf(req.getSalary(), taxable.getMiddleTaxRate()));
            case SENIOR -> new SeniorDeveloper(req.getId(), req.getName(),
                    netOf(req.getSalary(), taxable.getUpperTaxRate()));
        };

        developers.put(created.getId(), created);

        // Location: /developers/{id}
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/developers/{id}")
    public ResponseEntity<Developer> update(@PathVariable Integer id, @RequestBody Developer body) {
        if (!developers.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.getId() <= 0 || body.getName() == null || body.getSalary() <= 0 || body.getExperience() == null) {
            return ResponseEntity.badRequest().build();
        }
        // Basit yaklaşım: gelen body olduğu gibi geçerli kabul edilip map'e yazılır.
        developers.put(id, body);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/developers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        Developer removed = developers.remove(id);
        return (removed != null) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }


}
