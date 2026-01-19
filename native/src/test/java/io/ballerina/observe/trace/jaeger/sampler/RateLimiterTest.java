/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ballerina.observe.trace.jaeger.sampler;

import io.opentelemetry.sdk.common.Clock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test class for RateLimiter.
 */
public class RateLimiterTest {

    @Test
    public void testCheckCreditAllowsInitialRequest() {
        RateLimiter rateLimiter = new RateLimiter(2.0, 2.0, Clock.getDefault());
        assertTrue(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testCheckCreditAllowsMultipleRequestsWithinLimit() {
        RateLimiter rateLimiter = new RateLimiter(10.0, 10.0, Clock.getDefault());

        // Should allow multiple requests up to the balance
        assertTrue(rateLimiter.checkCredit(1.0));
        assertTrue(rateLimiter.checkCredit(1.0));
        assertTrue(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testCheckCreditDeniesExcessRequests() {
        RateLimiter rateLimiter = new RateLimiter(2.0, 2.0, Clock.getDefault());

        // Exhaust the balance
        assertTrue(rateLimiter.checkCredit(1.0));
        assertTrue(rateLimiter.checkCredit(1.0));

        // Next request should be denied
        assertFalse(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testCheckCreditWithHighRate() {
        RateLimiter rateLimiter = new RateLimiter(100.0, 100.0, Clock.getDefault());

        int allowedCount = 0;
        for (int i = 0; i < 150; i++) {
            if (rateLimiter.checkCredit(1.0)) {
                allowedCount++;
            }
        }

        // Should have allowed up to the max balance
        assertTrue(allowedCount > 0);
        assertTrue(allowedCount <= 100);
    }

    @Test
    public void testCheckCreditWithLowRate() {
        RateLimiter rateLimiter = new RateLimiter(0.1, 1.0, Clock.getDefault());

        // Should allow at least one request due to max balance
        assertTrue(rateLimiter.checkCredit(1.0));

        // Subsequent requests should be denied immediately
        assertFalse(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testCheckCreditRecoversOverTime() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(10.0, 10.0, Clock.getDefault());

        // Exhaust the balance
        for (int i = 0; i < 15; i++) {
            rateLimiter.checkCredit(1.0);
        }

        // Wait for recovery
        Thread.sleep(200);

        // Should be able to make requests again
        assertTrue(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testCheckCreditWithZeroCost() {
        RateLimiter rateLimiter = new RateLimiter(5.0, 5.0, Clock.getDefault());

        // Zero cost should always be allowed
        assertTrue(rateLimiter.checkCredit(0.0));
        assertTrue(rateLimiter.checkCredit(0.0));
        assertTrue(rateLimiter.checkCredit(0.0));
    }

    @Test
    public void testCheckCreditWithHighCost() {
        RateLimiter rateLimiter = new RateLimiter(10.0, 10.0, Clock.getDefault());

        // High cost request should consume more balance
        assertTrue(rateLimiter.checkCredit(5.0));

        // Should still have some balance left
        assertTrue(rateLimiter.checkCredit(4.0));

        // Should now be out of balance
        assertFalse(rateLimiter.checkCredit(2.0));
    }

    @Test
    public void testCheckCreditWithFractionalCost() {
        RateLimiter rateLimiter = new RateLimiter(10.0, 10.0, Clock.getDefault());

        assertTrue(rateLimiter.checkCredit(0.5));
        assertTrue(rateLimiter.checkCredit(0.5));
        assertTrue(rateLimiter.checkCredit(0.5));
    }

    @Test
    public void testCheckCreditConcurrentRequests() {
        RateLimiter rateLimiter = new RateLimiter(100.0, 100.0, Clock.getDefault());

        int allowedCount = 0;
        int deniedCount = 0;

        // Simulate concurrent requests
        for (int i = 0; i < 200; i++) {
            if (rateLimiter.checkCredit(1.0)) {
                allowedCount++;
            } else {
                deniedCount++;
            }
        }

        assertTrue(allowedCount > 0, "Some requests should be allowed");
        assertTrue(deniedCount > 0, "Some requests should be denied");
        assertTrue(allowedCount <= 100, "Should not exceed max balance");
    }

    @Test
    public void testRateLimiterWithMinimumRate() {
        // Even with very low rate, maxBalance ensures at least one request
        RateLimiter rateLimiter = new RateLimiter(0.01, 1.0, Clock.getDefault());

        assertTrue(rateLimiter.checkCredit(1.0));
    }

    @Test
    public void testRateLimiterBalanceCapping() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(10.0, 5.0, Clock.getDefault());

        // Use up some balance
        rateLimiter.checkCredit(2.0);

        // Wait longer than needed to fully recover
        Thread.sleep(1000);

        // Should be able to use max balance but not more
        int allowedCount = 0;
        for (int i = 0; i < 10; i++) {
            if (rateLimiter.checkCredit(1.0)) {
                allowedCount++;
            }
        }

        // Should be capped at max balance
        assertTrue(allowedCount <= 5);
    }
}

