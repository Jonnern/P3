package round_robin;

import java.util.LinkedList;

/**
 * This class implements functionality associated with
 * the CPU unit of the simulated system.
 */
public class Cpu {

    private LinkedList<Process> cpuQueue = new LinkedList<>();
    private long maxCpuTime;
    private Statistics statistics;
    private Process activeProcess = null;

    /**
     * Creates a new CPU with the given parameters.
     * @param cpuQueue		The CPU queue to be used.
     * @param maxCpuTime	The Round Robin time quant to be used.
     * @param statistics	A reference to the statistics collector.
     */
    public Cpu(LinkedList<Process> cpuQueue, long maxCpuTime, Statistics statistics) {
        this.cpuQueue = cpuQueue;
        this.maxCpuTime = maxCpuTime;
        this.statistics = statistics;
    }

    /**
     * Adds a process to the CPU queue, and activates (switches in) the first process
     * in the CPU queue if the CPU is idle.
     * @param p		The process to be added to the CPU queue.
     * @param clock	The global time.
     * @return		The event causing the process that was activated to leave the CPU,
     *				or null	if no process was activated.
     */
    public Event insertProcess(Process p, long clock) {
        cpuQueue.add(p);

        // Switching process if the CPU is idle
        if(this.getActiveProcess() == null){
            return switchProcess(clock);
        }
        return null;
    }

    /**
     * Activates (switches in) the first process in the CPU queue, if the queue is non-empty.
     * The process that was using the CPU, if any, is switched out and added to the back of
     * the CPU queue, in accordance with the Round Robin algorithm.
     * @param clock	The global time.
     * @return		The event causing the process that was activated to leave the CPU,
     *				or null	if no process was activated.
     */
    public Event switchProcess(long clock) {
        if(!cpuQueue.isEmpty()){
            /* Active process, if any, is put in the back of CpuQueue */
            if(activeProcess != null){
                activeProcess.leftCpu(clock);
                cpuQueue.add(activeProcess);
            }

            /* First process in queue is activated */
            activeProcess = cpuQueue.remove();
            activeProcess.leftReadyQueue(clock);

            /* Returning the event causing the process that was activated to leave the CPU */
            return generateEvent(clock);
        }

        /* If the queue is empty and a process was active, reactivate it */
        if (activeProcess != null){
            activeProcess.leftCpu(clock);
            /* Returning the event causing the process that was activated to leave the CPU */
            return generateEvent(clock);
        }

        return null;
    }

    /**
     * Called when the active process left the CPU (for example to perform I/O),
     * and a new process needs to be switched in.
     * @return	The event generated by the process switch, or null if no new
     *			process was switched in.
     */
    public Event activeProcessLeft(long clock) {
        //activeProcess.leftCpu(clock);
        activeProcess = null;
        return switchProcess(clock);
    }

    /**
     * Returns the process currently using the CPU.
     * @return	The process currently using the CPU.
     */
    public Process getActiveProcess() {
        return activeProcess;
    }

    /**
     * This method is called when a discrete amount of time has passed.
     * @param timePassed	The amount of time that has passed since the last call to this method.
     */
    public void timePassed(long timePassed) {
        // Updating statistics if the current queue is longer than the historical largest
        if(cpuQueue.size() > statistics.cpuQueueLargestLength){
            statistics.cpuQueueLargestLength = cpuQueue.size();
        }

        // Total CPU time has increased if the CPU is processing
        if(activeProcess != null){
            statistics.totalBusyCpuTime += timePassed;
        }

        // Updating time weighted queue length
        statistics.cpuQueueLengthTime += cpuQueue.size()*timePassed;
    }

    private Event generateEvent(long clock){
        // Creating END Event if the activated process finishes this time quanta
        if(activeProcess.getCpuTimeNeeded() < maxCpuTime && activeProcess.getCpuTimeNeeded() < activeProcess.getTimeToNextIoOperation()){
            return new Event(Event.END_PROCESS, clock + activeProcess.getCpuTimeNeeded());
        }

        // Creating IO Event if the activated process needs IO in this time quanta
        else if(activeProcess.getTimeToNextIoOperation() < maxCpuTime){
            return new Event(Event.IO_REQUEST, clock + activeProcess.getTimeToNextIoOperation());
        }

        // Creating Switch Event if the activated process does not finish this time quanta and does not need IO this time quanta
        else {
            return new Event(Event.SWITCH_PROCESS, clock + maxCpuTime);
        }
    }
}
