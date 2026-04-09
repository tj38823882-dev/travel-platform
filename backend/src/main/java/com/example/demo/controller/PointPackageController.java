package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.PointPackage;
import com.example.demo.repository.PointPackageRepository;

@RestController
@RequestMapping("/api/user")
public class PointPackageController {

    @Autowired
    private PointPackageRepository pointPackageRepository;

    @GetMapping("/packages")
    public List<PointPackage> getAllActivePackages() {
        System.out.println("Fetching all active packages...");
        return pointPackageRepository.findByIsActiveTrue();
    }
}
