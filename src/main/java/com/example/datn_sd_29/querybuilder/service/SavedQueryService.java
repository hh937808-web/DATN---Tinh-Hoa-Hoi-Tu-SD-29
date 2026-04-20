package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.QueryRequest;
import com.example.datn_sd_29.querybuilder.dto.SavedQueryRequest;
import com.example.datn_sd_29.querybuilder.entity.SavedQuery;
import com.example.datn_sd_29.querybuilder.repository.SavedQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedQueryService {
    
    private final SavedQueryRepository savedQueryRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public SavedQuery saveQuery(SavedQueryRequest request, Integer employeeId) {
        try {
            String queryJson = objectMapper.writeValueAsString(request.getQuery());
            
            SavedQuery savedQuery = new SavedQuery();
            savedQuery.setName(request.getName());
            savedQuery.setDescription(request.getDescription());
            savedQuery.setQueryDefinition(queryJson);
            savedQuery.setVisualizationType(request.getVisualizationType());
            savedQuery.setCreatedBy(employeeId);
            
            return savedQueryRepository.save(savedQuery);
        } catch (JsonProcessingException e) {
            log.error("Error serializing query", e);
            throw new RuntimeException("Failed to save query", e);
        }
    }
    
    @Transactional
    public SavedQuery updateQuery(Long queryId, SavedQueryRequest request, Integer employeeId) {
        SavedQuery savedQuery = savedQueryRepository.findById(queryId)
            .orElseThrow(() -> new RuntimeException("Query not found: " + queryId));
        
        // Check ownership
        if (!savedQuery.getCreatedBy().equals(employeeId)) {
            throw new SecurityException("You don't have permission to update this query");
        }
        
        try {
            String queryJson = objectMapper.writeValueAsString(request.getQuery());
            
            savedQuery.setName(request.getName());
            savedQuery.setDescription(request.getDescription());
            savedQuery.setQueryDefinition(queryJson);
            savedQuery.setVisualizationType(request.getVisualizationType());
            
            return savedQueryRepository.save(savedQuery);
        } catch (JsonProcessingException e) {
            log.error("Error serializing query", e);
            throw new RuntimeException("Failed to update query", e);
        }
    }
    
    @Transactional
    public void deleteQuery(Long queryId, Integer employeeId) {
        SavedQuery savedQuery = savedQueryRepository.findById(queryId)
            .orElseThrow(() -> new RuntimeException("Query not found: " + queryId));
        
        // Check ownership
        if (!savedQuery.getCreatedBy().equals(employeeId)) {
            throw new SecurityException("You don't have permission to delete this query");
        }
        
        savedQueryRepository.delete(savedQuery);
    }
    
    public List<SavedQuery> getMyQueries(Integer employeeId) {
        return savedQueryRepository.findByCreatedByOrderByUpdatedAtDesc(employeeId);
    }
    
    public List<SavedQuery> getAllQueries() {
        return savedQueryRepository.findAllByOrderByUpdatedAtDesc();
    }
    
    public SavedQuery getQuery(Long queryId) {
        return savedQueryRepository.findById(queryId)
            .orElseThrow(() -> new RuntimeException("Query not found: " + queryId));
    }
    
    public QueryRequest getQueryDefinition(Long queryId) {
        SavedQuery savedQuery = getQuery(queryId);
        try {
            return objectMapper.readValue(savedQuery.getQueryDefinition(), QueryRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing query", e);
            throw new RuntimeException("Failed to load query definition", e);
        }
    }
}
