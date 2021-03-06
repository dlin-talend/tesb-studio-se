// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2013 Talend – www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.esb.runcontainer.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.talend.designer.esb.runcontainer.logs.FelixLogsModel;
import org.talend.designer.esb.runcontainer.logs.RuntimeLogHTTPAdapter;
import org.talend.designer.esb.runcontainer.logs.RuntimeLogHTTPMonitor;

/**
 * DOC yyan class global comment. Detailled comment <br/>
 */
public class RunContainerProcess extends Process {

    private PipedOutputStream stdOutputStream;

    private PipedInputStream errInputStream;

    private PipedInputStream stdInputStream;

    private PipedOutputStream errOutputStream;

    private RuntimeLogHTTPAdapter logListener;

    private boolean startLogging;

    public RunContainerProcess() {
        stdInputStream = new PipedInputStream();
        errInputStream = new PipedInputStream();

        try {
            stdOutputStream = new PipedOutputStream(stdInputStream);
            errOutputStream = new PipedOutputStream(errInputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        logListener = new RuntimeLogHTTPAdapter() {

            @Override
            public synchronized void logReceived(FelixLogsModel logsModel) {
                if (startLogging) {
                    String msg = logsModel.toString();
                    try {
                        if ("INFO".equals(logsModel.getLevel())) {
                            stdOutputStream.write(msg.getBytes());
                            stdOutputStream.write('\n');
                            // stdOutputStream.flush();
                        } else {
                            errOutputStream.write(msg.getBytes());
                            errOutputStream.write('\n');
                            // errOutputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    // start logging
    public void startLogging() {
        RuntimeLogHTTPMonitor logMonitor = RuntimeLogHTTPMonitor.createRuntimeLogHTTPMonitor();
        logMonitor.startLogging();
        logMonitor.addLogLictener(logListener);
        startLogging = true;
    }

    public void stopLogging() {
        RuntimeLogHTTPMonitor.createRuntimeLogHTTPMonitor().removeLogLictener(logListener);
        startLogging = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() {
        // TODO Auto-generated method stub
        return stdOutputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#getInputStream()
     */
    @Override
    public InputStream getInputStream() {
        // TODO Auto-generated method stub
        return stdInputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#getErrorStream()
     */
    @Override
    public InputStream getErrorStream() {
        // TODO Auto-generated method stub
        return errInputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#waitFor()
     */
    @Override
    public int waitFor() throws InterruptedException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#exitValue()
     */
    @Override
    public int exitValue() {
        // TODO Auto-generated method stub
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Process#destroy()
     */
    @Override
    public void destroy() {
        stopLogging();
        try {
            stdInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            stdOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PipedOutputStream getErrOutputStream() {
        return errOutputStream;
    }

}
