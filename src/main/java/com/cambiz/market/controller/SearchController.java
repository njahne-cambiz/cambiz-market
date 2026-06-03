package com.cambiz.market.controller;

import com.cambiz.market.dto.SearchCriteria;
import com.cambiz.market.dto.SearchResultDTO;
import com.cambiz.market.service.AdvancedSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SearchController {
    
    @Autowired
    private AdvancedSearchService searchService;
    
    @GetMapping("/search")
    public String search(@ModelAttribute SearchCriteria criteria, Model model) {
        SearchResultDTO results = searchService.search(criteria);
        
        model.addAttribute("results", results);
        model.addAttribute("criteria", criteria);
        
        return "search/results";
    }
    
    @GetMapping("/api/search/suggestions")
    @ResponseBody
    public List<String> getSuggestions(@RequestParam String q) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeyword(q);
        criteria.setSize(5);
        return searchService.search(criteria).getSuggestedKeywords();
    }
}