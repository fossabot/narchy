/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.control;

import nars.config.Parameters;
import nars.entity.Concept;
import nars.entity.Task;
import nars.entity.TermLink;
import nars.inference.RuleTables;
import nars.storage.Memory;
import nars.util.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concept reasoning context - a concept is "fired" or activated by applying the reasoner */
public class GeneralInferenceControl {

    final static Logger logger = LoggerFactory.getLogger(GeneralInferenceControl.class);

    public static void selectConceptForInference(Memory mem) {
        Concept currentConcept = mem.concepts.takeNext();
        if (currentConcept==null)
            return;
        
        if(currentConcept.taskLinks.size() == 0) { //remove concepts without tasklinks and without termlinks
            mem.concepts.take(currentConcept.getTerm());
            mem.conceptRemoved(currentConcept);
            return;
        }
        if(currentConcept.termLinks.size() == 0) {  //remove concepts without tasklinks and without termlinks
            mem.concepts.take(currentConcept.getTerm());
            mem.conceptRemoved(currentConcept);
            return;
        }
        
        DerivationContext cont = new DerivationContext(mem);
        cont.setCurrentConcept(currentConcept);
        fireConcept(cont, 1);
    }
    
    public static void fireConcept(DerivationContext nal, int numTaskLinks) {     
        for (int i = 0; i < numTaskLinks; i++) {

            if (nal.currentConcept.taskLinks.size() == 0) 
                return;

            nal.currentTaskLink = nal.currentConcept.taskLinks.takeNext();                    
            if (nal.currentTaskLink == null)
                return;

            if (nal.currentTaskLink.budget.aboveThreshold()) {
                fireTaskLink(nal, Parameters.TERMLINK_MAX_REASONED);                    
            }

            nal.currentConcept.taskLinks.putBack(nal.currentTaskLink, nal.memory.cycles(nal.memory.param.taskLinkForgetDurations), nal.memory);
        }
        float forgetCycles = nal.memory.cycles(nal.memory.param.conceptForgetDurations);
        nal.memory.concepts.putBack(nal.currentConcept, forgetCycles, nal.memory);
    }
    
    protected static void fireTaskLink(DerivationContext nal, int termLinks) {
        final Task task = nal.currentTaskLink.getTarget();
        nal.setCurrentTerm(nal.currentConcept.term);
        nal.setCurrentTaskLink(nal.currentTaskLink);
        nal.setCurrentBeliefLink(null);
        nal.setCurrentTask(task); // one of the two places where this variable is set
        
        if (nal.currentTaskLink.type == TermLink.TRANSFORM) {
            nal.setCurrentBelief(null);
            
            RuleTables.transformTask(nal.currentTaskLink, nal); // to turn this into structural inference as below?
            
        } else {            
            while (termLinks > 0) {
                final TermLink termLink = nal.currentConcept.selectTermLink(nal.currentTaskLink, nal.memory.time());
                if (termLink == null) {
                    break;
                }
                fireTermlink(termLink, nal);
                nal.currentConcept.returnTermLink(termLink);
                termLinks--;
            }
        }
                
        nal.memory.emit(Events.ConceptFire.class, nal);
        //memory.logic.TASKLINK_FIRE.commit(currentTaskLink.budget.getPriority());
    }


    public static boolean fireTermlink(final TermLink termLink, DerivationContext nal) {
        nal.setCurrentBeliefLink(termLink);
        try {
            RuleTables.reason(nal.currentTaskLink, termLink, nal);
        } catch(Exception ex) {
            logger.error("fireTermLink: {}", ex);
            /*if(Parameters.DEBUG) {
                System.out.println("issue in inference");
            }*/
        }
        nal.memory.emit(Events.TermLinkSelect.class, termLink, nal.currentConcept, nal);
        //memory.logic.REASON.commit(termLink.getPriority());                    
        return true;
    }
}
