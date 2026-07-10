package com.ikms.client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {

  Optional<Client> findByClientIdIgnoreCase(String clientId);

  @Query("""
      select c
      from Client c
      where :query = ''
         or lower(c.displayName) like lower(concat('%', :query, '%'))
         or lower(c.clientId) like lower(concat('%', :query, '%'))
      order by c.displayName asc
      """)
  java.util.List<Client> searchByQuery(@Param("query") String query);
}
