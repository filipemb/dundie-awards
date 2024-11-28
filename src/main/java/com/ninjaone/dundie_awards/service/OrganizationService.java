package com.ninjaone.dundie_awards.service;

import java.util.List;
import java.util.UUID;

import com.ninjaone.dundie_awards.dto.OrganizationDto;
import com.ninjaone.dundie_awards.event.Event;
import com.ninjaone.dundie_awards.model.Organization;

public interface OrganizationService {

	boolean exists(Long id);

	Organization getOrganization(Long id);

	boolean isBlocked(Long id);

	Organization block(UUID uuid, Long id);

	List<OrganizationDto> getAllOrganizations();
	
	public void handleUnblockOrganizationSuccessEvent(Event event);
	public void handleUnblockOrganizationRetryEvent(Event event);
	public void handleUnblockOrganizationFailureEvent(Event event);

}