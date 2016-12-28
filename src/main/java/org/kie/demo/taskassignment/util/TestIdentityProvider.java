/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.demo.taskassignment.util;

import java.util.ArrayList;
import java.util.List;

import org.kie.internal.identity.IdentityProvider;

public class TestIdentityProvider implements IdentityProvider {

    private String name = "testUser";
    private List<String> roles = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    // just for testing
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setName(String name) {
        this.name = name;
    }
}
