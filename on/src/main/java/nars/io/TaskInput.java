/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.io;

import nars.entity.Task;

import java.io.IOException;

/**
 * Wraps a pre-created Task as an Input
 */
public class TaskInput implements Input {

    public final Task task;
    boolean finished;

    public TaskInput(Task t) {
        this.task = t;
    }
    
    
    @Override
    public Object next() throws IOException {
        if (!finished) {
            finished = true;
            return task;        
        }
        return null;
    }

    @Override
    public boolean finished(boolean stop) {
        if (stop) finished = true;
        return finished;
    }
    
    
    
    
}
