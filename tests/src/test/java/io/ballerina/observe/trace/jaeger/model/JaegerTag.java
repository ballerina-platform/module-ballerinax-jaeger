/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.trace.jaeger.model;

import java.util.Objects;

/**
 * Jaeger Trace Tag model.
 */
public class JaegerTag {
    private String key;
    private String type;
    private String value;

    public JaegerTag(String key, String type, String value) {
        this.key = key;
        this.type = type;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JaegerTag)) {
            return false;
        }
        JaegerTag jaegerTag = (JaegerTag) o;
        return Objects.equals(getKey(), jaegerTag.getKey()) &&
                Objects.equals(getType(), jaegerTag.getType()) &&
                Objects.equals(getValue(), jaegerTag.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getType(), getValue());
    }

    @Override
    public String toString() {
        return "JaegerTag{" +
                "key='" + key + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
