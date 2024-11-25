package com.ninjaone.dundie_awards.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ninjaone.dundie_awards.AwardsCache;
import com.ninjaone.dundie_awards.MessageBroker;
import com.ninjaone.dundie_awards.service.ActivityService;
import com.ninjaone.dundie_awards.service.EmployeeService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {

	private final EmployeeService employeeService;
    private final ActivityService activityService;
    private final MessageBroker messageBroker;
    private final AwardsCache awardsCache;
    
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @GetMapping
    public String getIndex(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("activities", activityService.getAllActivities());
        model.addAttribute("queueMessages", messageBroker.getMessages());
        model.addAttribute("totalDundieAwards", awardsCache.getTotalAwards());
        model.addAttribute("isDev", "dev".equalsIgnoreCase(activeProfile));
        return "index";
    }
    
}