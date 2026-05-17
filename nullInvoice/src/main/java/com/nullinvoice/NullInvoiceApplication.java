// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice;

import com.nullinvoice.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class NullInvoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NullInvoiceApplication.class, args);
    }
}
