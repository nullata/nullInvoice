// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.repository;

import com.nullinvoice.entity.ApiKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKeys, Long> {

    List<ApiKeys> findAllByEnabledTrueOrderByCreatedAtDesc();

    List<ApiKeys> findAllByOrderByCreatedAtDesc();
}
