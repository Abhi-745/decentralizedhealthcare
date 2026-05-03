package com.example.VeristasId.Repository;

import com.example.VeristasId.Model.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    Optional<Consent> findByPatientDidAndDelegateDidAndActiveTrue(
            String patientDid, String delegateDid);

    List<Consent> findAllByPatientDid(String patientDid);
}