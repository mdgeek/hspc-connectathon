package org.hspconsortium.cwfdemo.api.democonfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.api.context.ContextItems;
import org.carewebframework.api.context.ContextManager;
import org.carewebframework.api.context.IContextEvent;
import org.carewebframework.api.context.ManagedContext;

/**
 * Wrapper for shared scenario context.
 */
public class ScenarioContext extends ManagedContext<Scenario> {
    
    private static final String SUBJECT_NAME = "Scenario";
    
    private static final Log log = LogFactory.getLog(ScenarioContext.class);
    
    public interface IScenarioContextEvent extends IContextEvent {};
    
    private final ScenarioRegistry registry;
    
    /**
     * Returns the managed scenario context.
     * 
     * @return Scenario context.
     */
    public static ScenarioContext getScenarioContext() {
        return (ScenarioContext) ContextManager.getInstance().getSharedContext(ScenarioContext.class.getName());
    }
    
    /**
     * Request a scenario context change.
     * 
     * @param scenario New scenario.
     */
    public static void changeScenario(Scenario scenario) {
        try {
            getScenarioContext().requestContextChange(scenario);
        } catch (Exception e) {
            log.error("Error during scenario context change.", e);
        }
    }
    
    /**
     * Request a scenario context change.
     * 
     * @param name Name of the scenario.
     */
    public static void changeScenario(String name) {
        ScenarioContext ctx = getScenarioContext();
        ctx.requestContextChange(ctx.registry.get(name));
    }
    
    /**
     * Returns the scenario in the current context.
     * 
     * @return Scenario object (may be null).
     */
    public static Scenario getActiveScenario() {
        return getScenarioContext().getContextObject(false);
    }
    
    /**
     * Create a shared scenario context with an initial null state.
     * 
     * @param registry Scenario registry for lookups by name.
     */
    public ScenarioContext(ScenarioRegistry registry) {
        super(SUBJECT_NAME, IScenarioContextEvent.class);
        this.registry = registry;
    }
    
    /**
     * Not implemented
     */
    @Override
    public ContextItems toCCOWContext(Scenario scenario) {
        return null;
    }
    
    /**
     * Not implemented
     */
    @Override
    public Scenario fromCCOWContext(ContextItems contextItems) {
        return null;
    }
    
}
