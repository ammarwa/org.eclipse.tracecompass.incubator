/*******************************************************************************
 * Copyright (c) 2024 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.handlers;

import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.incubator.gpu.core.analysis.GpuCallStackAnalysis;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout.IApiEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Handler for All APIs used to pilot the GPU.
 *
 * This handler handles all API calls that are written to call stacks.
 */
public class ROCpdMemAllocEventHandler implements IGpuEventHandler {

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, IGpuTraceEventLayout layout, ITmfStateProvider stateProvider) {
        String agentType = event.getContent().getFieldValue(String.class, "agent_type"); //$NON-NLS-1$
        boolean isGPU = false;
        if (agentType != null && agentType.contains("GPU")) { //$NON-NLS-1$
            isGPU = true;
        }
        Long agentId = event.getContent().getFieldValue(Long.class, "agent_abs_index"); //$NON-NLS-1$
        if (agentId == null) {
            return;
        }
        Long allocation_id = event.getContent().getFieldValue(Long.class, "allocation_id"); //$NON-NLS-1$
        if (allocation_id == null) {
            return;
        }
        Long pid = event.getContent().getFieldValue(Long.class, "pid"); //$NON-NLS-1$
        if (pid == null) {
            return;
        }
        Long streamId = event.getContent().getFieldValue(Long.class, "stream_id"); //$NON-NLS-1$
        if (streamId == null) {
            return;
        }
        Long tid = event.getContent().getFieldValue(Long.class, layout.fieldThreadId());
        if (tid == null) {
            return;
        }
        int rootQuark = ssb.getQuarkAbsoluteAndAdd(GpuCallStackAnalysis.ROOT, "Process: " + pid.toString()); //$NON-NLS-1$
        int threadQuark = ssb.getQuarkRelativeAndAdd(rootQuark, "Thread: " + tid.toString()); //$NON-NLS-1$

        int agentQuark = 0;

        if(isGPU) {
            int streamQuark = ssb.getQuarkRelativeAndAdd(threadQuark, "Stream: " + streamId.toString()); //$NON-NLS-1$
            agentQuark = ssb.getQuarkRelativeAndAdd(streamQuark, "Agent: " + agentId.toString()); //$NON-NLS-1$
        } else {
            int cpuTraceQuark = ssb.getQuarkRelativeAndAdd(threadQuark, "CPU Trace"); //$NON-NLS-1$
            agentQuark = cpuTraceQuark;
        }

        IApiEventLayout apiLayout = layout.getCorrespondingApiLayout(event);
        int callStackQuark = ssb.getQuarkRelativeAndAdd(agentQuark, CallStackAnalysis.CALL_STACK);

        if (apiLayout.isBeginEvent(event)) {
            ssb.pushAttribute(event.getTimestamp().getValue(), "Memory Allocation: ID: " + allocation_id, callStackQuark); //$NON-NLS-1$
        } else {
            ssb.popAttribute(event.getTimestamp().getValue(), callStackQuark);
        }
    }
}
