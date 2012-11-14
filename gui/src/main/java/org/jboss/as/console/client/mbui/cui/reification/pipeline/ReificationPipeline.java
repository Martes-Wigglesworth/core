/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.mbui.cui.reification.pipeline;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.mbui.aui.aim.InteractionUnit;
import org.jboss.as.console.client.mbui.cui.Context;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Harald Pehl
 * @date 11/12/2012
 */
public class ReificationPipeline
{
    private final List<ReificationStep> steps;

    @Inject
    public ReificationPipeline(ReadResourceDescriptionStep readResourceDescriptionStep, BuildUserInterfaceStep buildUserInterfaceStep)
    {
        // order is important!
        this.steps = new LinkedList<ReificationStep>();
        this.steps.add(readResourceDescriptionStep);
        this.steps.add(buildUserInterfaceStep);
    }

    public void execute(
            final InteractionUnit interactionUnit,
            final Context context,
            final AsyncCallback<Boolean> outcome)
    {
        for (ReificationStep step : steps)
        {
            step.init(interactionUnit, context);
        }

        Iterator<ReificationStep> iterator = steps.iterator();
        iterator.next().execute(iterator, new ReificationCallback() {

            int numResponses;
            boolean overallResult;

            @Override
            public void onSuccess(Boolean successful) {
                numResponses++;
                overallResult = successful;

                if(numResponses==steps.size())
                {
                    outcome.onSuccess(overallResult);
                }
            }
        });
    }

}
