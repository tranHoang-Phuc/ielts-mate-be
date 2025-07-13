package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ListeningExamRepository extends JpaRepository<ListeningExam, UUID> {

}
