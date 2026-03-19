package com.example.controller;

import java.sql.SQLException;

import com.example.model.PlanningResult;
import com.example.service.PlanningService;
import com.myframework.annotations.Controller;
import com.myframework.annotations.GetMapping;
import com.myframework.annotations.RequestParam;
import com.myframework.core.ModelView;

@Controller
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController() {
        this.planningService = new PlanningService();
    }

    @GetMapping("/planification")
    public ModelView showPlanningDateForm() {
        return new ModelView("/planning-date.jsp");
    }

    @GetMapping("/planification/result")
    public ModelView showPlanningResult(@RequestParam("date") String date) throws SQLException {
        ModelView mv;

        try {
            PlanningResult planning = planningService.planifier(date);
            mv = new ModelView("/planning-result.jsp");
            mv.addData("planning", planning);
        } catch (IllegalArgumentException e) {
            mv = new ModelView("/planning-date.jsp");
            mv.addData("errorMessage", e.getMessage());
            mv.addData("planningDate", date);
        }

        return mv;
    }
}