/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor.loadbalancer;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.ObjectHelper;

/**
 * This FailOverLoadBalancer will failover to use next processor when an exception occured
 */
public class FailOverLoadBalancer extends LoadBalancerSupport {

    private final Class failException;

    public FailOverLoadBalancer(Class throwable) {
        if (ObjectHelper.isAssignableFrom(Throwable.class, throwable)) {
            failException = throwable;
        } else {
            throw new IllegalArgumentException("Class is not an instance of Trowable: " + throwable);
        }
    }

    public FailOverLoadBalancer() {
        this(Throwable.class);
    }

    protected boolean isCheckedException(Exchange exchange) {
        if (exchange.getException() != null) {
            if (failException.isAssignableFrom(exchange.getException().getClass())) {
                return true;
            }
        }
        return false;
    }

    public void process(Exchange exchange) throws Exception {
        List<Processor> list = getProcessors();
        if (list.isEmpty()) {
            throw new IllegalStateException("No processors available to process " + exchange);
        }
        int index = 0;
        Processor processor = list.get(index);
        processExchange(processor, exchange);
        while (isCheckedException(exchange)) {
            exchange.setException(null);
            index++;
            if (index < list.size()) {
                processor = list.get(index);
                processExchange(processor, exchange);
            } else {
                break;
            }
        }
    }

    private void processExchange(Processor processor, Exchange exchange) {
        if (processor == null) {
            throw new IllegalStateException("No processors could be chosen to process " + exchange);
        }
        try {
            processor.process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

}
