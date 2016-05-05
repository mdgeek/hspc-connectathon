/*
 * #%L
 * UCS Messaging API
 * %%
 * Copyright (C) 2014 - 2016 Healthcare Services Platform Consortium
 * %%
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
 * #L%
 */
package org.hspconsortium.cwfdemo.api.ucs;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBroadcaster<T> {
    
    
    protected final List<T> listeners = new ArrayList<>();
    
    public AbstractBroadcaster() {
    }
    
    /**
     * Constructor takes initial list of listeners to whom events will be broadcasted.
     * 
     * @param listeners Initial set of listeners.
     */
    public AbstractBroadcaster(List<T> listeners) {
        this.listeners.addAll(listeners);
    }
    
    /**
     * Registers a listener with the broadcaster
     * 
     * @param listener Listener to register
     */
    public void registerListener(T listener) {
        this.listeners.add(listener);
    }
    
    /**
     * Unregisters a listener from the broadcaster
     * 
     * @param listener Listener to unregister
     */
    public void unregisterListener(T listener) {
        this.listeners.remove(listener);
    }
    
}
