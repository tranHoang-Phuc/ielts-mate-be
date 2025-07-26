package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.FlashCard;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface FlashCardRepository  extends CrudRepository<FlashCard, UUID> {
}
