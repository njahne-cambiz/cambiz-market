package com.cambiz.market.controller;

import com.cambiz.market.dto.SearchCriteria;
import com.cambiz.market.dto.SearchResultDTO;
import com.cambiz.market.service.AdvancedSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Controller
public class SearchController {
    
    @Autowired
    private AdvancedSearchService searchService;
    
    @GetMapping("/search")
    public String search(@ModelAttribute SearchCriteria criteria, Model model) {
        try {
            if (criteria == null) {
                criteria = new SearchCriteria();
            }
            SearchResultDTO results = searchService.search(criteria);
            model.addAttribute("results", results);
            model.addAttribute("criteria", criteria);
        } catch (Exception e) {
            model.addAttribute("results", new SearchResultDTO());
            model.addAttribute("criteria", new SearchCriteria());
            model.addAttribute("error", e.getMessage());
        }
        return "search/results";
    }
}