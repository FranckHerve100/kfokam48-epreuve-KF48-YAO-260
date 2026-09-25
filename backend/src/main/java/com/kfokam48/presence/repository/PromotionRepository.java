package com.kfokam48.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
