package com.example.datn_sd_29.media.repository;

import com.example.datn_sd_29.media.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {


    List<Image> findByProduct_IdOrderByIsPrimaryDesc(Integer productId);


    Optional<Image> findByProduct_IdAndIsPrimaryTrue(Integer productId);



    List<Image> findByProductCombo_IdOrderByIsPrimaryDesc(Integer comboId);


    Optional<Image> findByProductCombo_IdAndIsPrimaryTrue(Integer comboId);

}