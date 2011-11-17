package org.jboss.as.console.client.shared.general.validation;

import org.jboss.as.console.client.shared.general.model.Interface;
import org.jboss.ballroom.client.widgets.forms.FormItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 11/16/11
 */
abstract class AbstractValidationStep<T> implements ValidationStep<T> {

    static Decision SUCCESS = new Decision<Interface>() {
        @Override
        public boolean evaluate(Interface entity) {
            return true;
        }
    };
    static Decision FAILURE = new Decision<Interface>() {
        @Override
        public boolean evaluate(Interface entity) {
            return false;
        }
    };

    private DecisionTree.DecisionLog decisionLog = null;

    public void setLog(DecisionTree.DecisionLog decisionLog) {
        this.decisionLog = decisionLog;
    }

    @Override
    public ValidationResult validate(T entity, Map<String, Object> changedValues) {

        DecisionTree<T> tree = buildDecisionTree(entity, changedValues);
        tree.setDecisionLog(decisionLog);

        //tree.outputBinTree();
        tree.queryBinTree();

        // create result
        ValidationResult result = new ValidationResult(tree.getFinalOutcome());
        result.addMessage(tree.getLastNode().getQuestOrAns());
        return result;
    }

    protected abstract DecisionTree<T> buildDecisionTree(T entity, Map<String,Object> changedValues);

    public static boolean isSet(String value)
    {
        return value!=null && !value.isEmpty();
    }

    protected static Map<String, Object> clearChangeset(Map<String, Object> changedValues) {

        Map<String,Object> clean = new HashMap<String, Object>();
        Set<String> keys = changedValues.keySet();
        for(String key : keys)
        {
            Object value = changedValues.get(key);
            if(! (value == FormItem.VALUE_SEMANTICS.UNDEFINED))
                clean.put(key, value);
        }
        return clean;
    }

    protected static boolean isEmpty(Map<String, Object> changedValues) {

        boolean empty = changedValues.isEmpty();

        if(!empty)
        {
            // treat any boolean=false as empty too
            // it will written as undefined
            boolean conflictingBoolean = false;
            Set<String> keys = changedValues.keySet();
            for(String key : keys)
            {
                Object value = changedValues.get(key);
                if("true".equals(value.toString())) // TODO: expensive, improve
                {
                    conflictingBoolean = true;
                    break;
                }
            }

            empty = !conflictingBoolean;
        }

        return empty;
    }
}
