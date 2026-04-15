package com.sensedia.consent.mapper;

import com.sensedia.consent.domain.Consent;
import com.sensedia.consent.dto.ConsentCreateRequest;
import com.sensedia.consent.dto.ConsentResponse;
import com.sensedia.consent.dto.ConsentUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ConsentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDateTime", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    Consent toEntity(ConsentCreateRequest request);

    ConsentResponse toResponse(Consent consent);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDateTime", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    void updateEntityFromRequest(ConsentUpdateRequest request, @MappingTarget Consent consent);
}
