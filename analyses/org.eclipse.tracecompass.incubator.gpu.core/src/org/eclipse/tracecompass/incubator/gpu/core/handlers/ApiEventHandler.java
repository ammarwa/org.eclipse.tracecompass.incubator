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
public class ApiEventHandler implements IGpuEventHandler {

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, IGpuTraceEventLayout layout, ITmfStateProvider stateProvider) {
        Long tid = event.getContent().getFieldValue(Long.class, layout.fieldThreadId());
        Long pid = event.getContent().getFieldValue(Long.class, "pid"); //$NON-NLS-1$
        if (tid == null) {
            return;
        }
        if (pid == null) {
            return;
        }
        Long region_id = event.getContent().getFieldValue(Long.class, "region_id"); //$NON-NLS-1$
        if (region_id == null) {
            return;
        }
        Long streamId = event.getContent().getFieldValue(Long.class, "stream_id"); //$NON-NLS-1$
        int rootQuark = ssb.getQuarkAbsoluteAndAdd(GpuCallStackAnalysis.ROOT, "Process: " + pid.toString()); //$NON-NLS-1$
        int threadQuark = ssb.getQuarkRelativeAndAdd(rootQuark, "Thread: " + tid.toString()); //$NON-NLS-1$
        int cpuTraceQuark = ssb.getQuarkRelativeAndAdd(threadQuark, "CPU Trace"); //$NON-NLS-1$
        int streamQuark = cpuTraceQuark;
        if (streamId != null) {
            streamQuark = ssb.getQuarkRelativeAndAdd(cpuTraceQuark, "Stream: " + streamId.toString()); //$NON-NLS-1$
        }

        IApiEventLayout apiLayout = layout.getCorrespondingApiLayout(event);
        int callStackQuark = ssb.getQuarkRelativeAndAdd(streamQuark, CallStackAnalysis.CALL_STACK);

        if (apiLayout.isBeginEvent(event)) {
            ssb.pushAttribute(event.getTimestamp().getValue(), region_id + ": " + apiLayout.getEventName(event), callStackQuark); //$NON-NLS-1$
        } else {
            ssb.popAttribute(event.getTimestamp().getValue(), callStackQuark);
        }
    }
}
