package com.a1systems.smpp.simulator;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptException;

class TickTask extends TimerTask {

    protected Application app;
    protected SimulatorSession simSession;

    public TickTask(Application app, SimulatorSession simSession) {
        this.app = app;
        this.simSession = simSession;
    }

    @Override
    public void run() {
        if (app.getInvocableEngine()!=null) {
            Invocable invocableEngine = app.getInvocableEngine();

            try {
                invocableEngine.invokeFunction(ScriptConstants.HANDLER_ON_TICK, simSession);
            } catch (ScriptException ex) {
                Logger.getLogger(TickTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(TickTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
