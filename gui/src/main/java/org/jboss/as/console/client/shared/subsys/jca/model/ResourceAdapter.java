package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.ballroom.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 7/19/11
 */
public interface ResourceAdapter {
     String getName();
    void setName(String name);

    @Binding(detypedName = "jndi-name")
    String getJndiName();
    void setJndiName(String name);

    @Binding(detypedName = "pool-name")
    String getPoolName();
    void setPoolName(String name);

    // regular DS attributes below

    @Binding(detypedName = "class-name")
    String getConnectionClass();
    void setConnectionClass(String classname);

    @Binding(detypedName = "transaction-support")
    String getTransactionSupport();
    void setTransactionSupport(String txSupport);

    String getArchive();
    void setArchive(String archive);
}
