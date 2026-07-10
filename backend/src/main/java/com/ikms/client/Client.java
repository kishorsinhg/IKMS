package com.ikms.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client")
public class Client {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false, unique = true, length = 80)
  private String clientId;

  @Column(name = "client_id_temporary", nullable = false)
  private boolean clientIdTemporary;

  @Enumerated(EnumType.STRING)
  @Column(name = "client_type", nullable = false, length = 32)
  private ClientType clientType = ClientType.INDIVIDUAL;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ClientStatus status = ClientStatus.ACTIVE;

  @Column(name = "display_name", nullable = false, length = 200)
  private String displayName;

  @Column(name = "legal_name", length = 200)
  private String legalName;

  @Column(name = "primary_email", length = 255)
  private String primaryEmail;

  @Column(name = "secondary_email", length = 255)
  private String secondaryEmail;

  @Column(name = "primary_phone", length = 64)
  private String primaryPhone;

  @Column(name = "secondary_phone", length = 64)
  private String secondaryPhone;

  @Column(name = "contact_person", length = 255)
  private String contactPerson;

  @Column(name = "address_line_1", length = 255)
  private String addressLine1;

  @Column(name = "address_line_2", length = 255)
  private String addressLine2;

  @Column(name = "city", length = 120)
  private String city;

  @Column(name = "state_or_region", length = 120)
  private String stateOrRegion;

  @Column(name = "postal_code", length = 32)
  private String postalCode;

  @Column(name = "country", length = 120)
  private String country;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = updatedAt == null ? now : updatedAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public boolean isClientIdTemporary() {
    return clientIdTemporary;
  }

  public void setClientIdTemporary(boolean clientIdTemporary) {
    this.clientIdTemporary = clientIdTemporary;
  }

  public ClientType getClientType() {
    return clientType;
  }

  public void setClientType(ClientType clientType) {
    this.clientType = clientType;
  }

  public ClientStatus getStatus() {
    return status;
  }

  public void setStatus(ClientStatus status) {
    this.status = status;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getLegalName() {
    return legalName;
  }

  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  public String getPrimaryEmail() {
    return primaryEmail;
  }

  public void setPrimaryEmail(String primaryEmail) {
    this.primaryEmail = primaryEmail;
  }

  public String getSecondaryEmail() {
    return secondaryEmail;
  }

  public void setSecondaryEmail(String secondaryEmail) {
    this.secondaryEmail = secondaryEmail;
  }

  public String getPrimaryPhone() {
    return primaryPhone;
  }

  public void setPrimaryPhone(String primaryPhone) {
    this.primaryPhone = primaryPhone;
  }

  public String getSecondaryPhone() {
    return secondaryPhone;
  }

  public void setSecondaryPhone(String secondaryPhone) {
    this.secondaryPhone = secondaryPhone;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getStateOrRegion() {
    return stateOrRegion;
  }

  public void setStateOrRegion(String stateOrRegion) {
    this.stateOrRegion = stateOrRegion;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
