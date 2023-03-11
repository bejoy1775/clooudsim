/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.util.*;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM management, as vm
 * creation, submission of cloudlets to VMs and destruction of VMs.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class DatacenterBroker extends SimEntity {

	/** The list of VMs submitted to be managed by the broker. */
	protected List<? extends Vm> vmList;

	/** The list of VMs created by the broker. */
	protected List<? extends Vm> vmsCreatedList;

	/** The list of cloudlet submitted to the broker. 
         * @see #submitCloudletList(java.util.List) 
         */
	protected List<? extends Cloudlet> cloudletList;

	/** The list of submitted cloudlets. */
	protected List<? extends Cloudlet> cloudletSubmittedList;

	/** The list of received cloudlet. */
	protected List<? extends Cloudlet> cloudletReceivedList;

	/** The number of submitted cloudlets. */
	protected int cloudletsSubmitted;

	/** The number of requests to create VM. */
	protected int vmsRequested;

	/** The number of acknowledges (ACKs) sent in response to
         * VM creation requests. */
	protected int vmsAcks;

	/** The number of destroyed VMs. */
	protected int vmsDestroyed;

	/** The id's list of available datacenters. */
	protected List<Integer> datacenterIdsList;

	/** The list of datacenters where was requested to place VMs. */
	protected List<Integer> datacenterRequestedIdsList;

	/** The vms to datacenters map, where each key is a VM id
         * and each value is the datacenter id whwere the VM is placed. */
	protected Map<Integer, Integer> vmsToDatacentersMap;

	/** The datacenter characteristics map where each key
         * is a datacenter id and each value is its characteristics.. */
	protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;

	/**
	 * Created a new DatacenterBroker object.
	 * 
	 * @param name name to be associated with this entity (as required by {@link SimEntity} class)
	 * @throws Exception the exception
	 * @pre name != null
	 * @post $none
	 */
	public DatacenterBroker(String name) throws Exception {
		super(name);

		setVmList(new ArrayList<Vm>());
		setVmsCreatedList(new ArrayList<Vm>());
		setCloudletList(new ArrayList<Cloudlet>());
		setCloudletSubmittedList(new ArrayList<Cloudlet>());
		setCloudletReceivedList(new ArrayList<Cloudlet>());

		cloudletsSubmitted = 0;
		setVmsRequested(0);
		setVmsAcks(0);
		setVmsDestroyed(0);

		setDatacenterIdsList(new LinkedList<Integer>());
		setDatacenterRequestedIdsList(new ArrayList<Integer>());
		setVmsToDatacentersMap(new HashMap<Integer, Integer>());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
	}

	/**
	 * This method is used to send to the broker the list with virtual machines that must be
	 * created.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	public void submitVmList(List<? extends Vm> list) {
		getVmList().addAll(list);
	}

	/**
	 * This method is used to send to the broker the list of cloudlets.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
         * 
         * @todo The name of the method is confused with the {@link #submitCloudlets()},
         * that in fact submit cloudlets to VMs. The term "submit" is being used
         * ambiguously. The method {@link #submitCloudlets()} would be named "sendCloudletsToVMs"
         * 
         * The method {@link #submitVmList(java.util.List)} may have
         * be checked too.
	 */
	public void submitCloudletList(List<? extends Cloudlet> list) {
		getCloudletList().addAll(list);
	}

	/**
	 * Specifies that a given cloudlet must run in a specific virtual machine.
	 * 
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	public void bindCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
	}

	////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Extended function for allocating cloudlets to vms in order.
	 */
	public void bindCloudletsToVmsSimple(){
		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		int idx=0;
		for(int i=0;i<cloudletNum;i++){
			cloudletList.get(i).setVmId(vmList.get(idx).getId());
			idx=(idx+1)%vmNum;
		}
	}
	/**
	 * Extended function for allocating cloudlets to vms
	 */


	public void bindCloudletToVmsScheduling(){
		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		double[] vmLoad=new double[vmNum];
		int idx=0;

		cloudletList.get(0).setVmId(vmList.get(0).getId());
		vmLoad[0]+=cloudletList.get(0).getCloudletLength()/vmList.get(0).getMips();
		for(int j=1;j<cloudletNum;j++){
			for(int i=0;i<vmNum;i++){
				if(vmLoad[i]<vmLoad[idx]){
					idx=i;
				}}
			cloudletList.get(j).setVmId(vmList.get(idx).getId());
			vmLoad[idx]+=cloudletList.get(j).getCloudletLength()/vmList.get(idx).getMips();
		}
	}

	public void bindCloudletToVmsMinMin(){

		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		double[] readyTime = new double[vmNum];
		double[][] expectedExecutionTime = new double[cloudletNum][vmNum];
		List<Integer> unassignedTasks =  new LinkedList<>();

		for(int j=0;j<cloudletNum;j++){
			for(int i=0;i<vmNum;i++){
				expectedExecutionTime[j][i] = cloudletList.get(j).getCloudletLength()/vmList.get(i).getMips()  + readyTime[i];
			}
			unassignedTasks.add(cloudletList.get(j).getCloudletId());
		}

		while (!unassignedTasks.isEmpty()) {

			double earliestCompletionTime = Double.MAX_VALUE;
			int machine = 0;
			int task = 0;
			for(int j=0;j<cloudletNum;j++){
				if (unassignedTasks.contains(j)) {
					for (int i = 0; i < vmNum; i++) {
						if (expectedExecutionTime[j][i] < earliestCompletionTime) {
							machine = i;
							task = j;
							earliestCompletionTime = expectedExecutionTime[j][i];
						}
					}
				}
			}
			cloudletList.get(task).setVmId(vmList.get(machine).getId());
			unassignedTasks.remove(unassignedTasks.indexOf(task));
			readyTime[machine] = readyTime[machine] + expectedExecutionTime[task][machine];
			for (int j = 0; j < cloudletNum; j++) {
				expectedExecutionTime[j][machine] = expectedExecutionTime[j][machine] + readyTime[machine];
			}
		}
	}

	public void bindCloudletToVmsMaxMin(){

		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		double[] readyTime = new double[vmNum];
		List<List<Double>> exTime = new LinkedList<>();
		List<Integer> unassignedTasks =  new LinkedList<>();

		for(int j=0;j<cloudletNum;j++){
			List<Double> vm  = new LinkedList<>();
			for(int i=0;i<vmNum;i++){
				vm.add(cloudletList.get(j).getCloudletLength()/vmList.get(i).getMips()  + readyTime[i]);
			}
			exTime.add(j, vm);
			unassignedTasks.add(cloudletList.get(j).getCloudletId());
		}

		while (!unassignedTasks.isEmpty()) {

			double earliestCompletionTime = Double.MAX_VALUE;
			int machine = 0;
			int task = 0;
			Map<Integer, Double> minET = new HashMap<>();
			for(int j=0;j<cloudletNum;j++){
				if (unassignedTasks.contains(j)) {
					for (int i = 0; i < vmNum; i++) {
						if (exTime.get(j).get(i) < earliestCompletionTime) {
							machine = i;
							earliestCompletionTime = exTime.get(j).get(i);
						}
					}
				}
				minET.put(j, earliestCompletionTime);
				earliestCompletionTime = Double.MAX_VALUE;
			}
			double maxTaskValue =  Double.MIN_VALUE;

			for (Map.Entry<Integer, Double> e : minET.entrySet()) {
				if  (unassignedTasks.contains(e.getKey())) {
					if (e.getValue() > maxTaskValue) {
						maxTaskValue = e.getValue();
						task = e.getKey();
					}
				}
			}

			cloudletList.get(task).setVmId(vmList.get(machine).getId());

			readyTime[machine] = readyTime[machine] + exTime.get(task).get(machine);
			for (int j = 0; j < exTime.size(); j++) {
				exTime.get(j).set(machine, exTime.get(j).get(machine) + readyTime[machine]);
			}

			unassignedTasks.remove(unassignedTasks.indexOf(task));

		}
	}

	public void bindCloudletToVmsSufferage() {
		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		double[] readyTime = new double[vmNum];
		double[][] expectedExecutionTime = new double[cloudletNum][vmNum];
		List<Integer> unassignedTasks =  new LinkedList<>();

		for(int j=0;j<cloudletNum;j++){
			for(int i=0;i<vmNum;i++){
				expectedExecutionTime[j][i] = cloudletList.get(j).getCloudletLength()/vmList.get(i).getMips()  + readyTime[i];
			}
			unassignedTasks.add(cloudletList.get(j).getCloudletId());
		}

		while (!unassignedTasks.isEmpty()) {

			int machine = 0;
			int task = 0;
			double sufferageValue = 0;
			Map<Integer, Integer> machineTask = new HashMap<>();
			Map<Integer, Double> machineSufferage = new HashMap<>();

			for(int j=0;j<cloudletNum;j++){
				double previousCompletionTime = Double.MAX_VALUE;
				double earliestCompletionTime = Double.MAX_VALUE;
				if (unassignedTasks.contains(j)) {

					for (int i = 0; i < vmNum; i++) {
						if ((expectedExecutionTime[j][i] > earliestCompletionTime)
							&& (expectedExecutionTime[j][i] < previousCompletionTime)) {
							previousCompletionTime = expectedExecutionTime[j][i];
						}

						if (expectedExecutionTime[j][i] < earliestCompletionTime) {
							machine = i;
							task = j;
							previousCompletionTime = earliestCompletionTime;
							earliestCompletionTime = expectedExecutionTime[j][i];
						}
					}

					sufferageValue = previousCompletionTime - earliestCompletionTime;

					if (!machineTask.containsKey(machine)) {
						machineTask.put(machine, task);
						machineSufferage.put(machine, sufferageValue);
					}
					else {
						if (sufferageValue > machineSufferage.get(machine)) {
							machineTask.put(machine, task);
							machineSufferage.put(machine, sufferageValue);
						}
					}
				}
			}

			for (Map.Entry<Integer, Integer> e : machineTask.entrySet()) {
				cloudletList.get(e.getValue()).setVmId(vmList.get(e.getKey()).getId());
				unassignedTasks.remove(unassignedTasks.indexOf(e.getValue()));
				readyTime[e.getKey()] = expectedExecutionTime[e.getValue()][e.getKey()];
				for (int j = 0; j < cloudletNum; j++) {
						expectedExecutionTime[j][e.getKey()] = expectedExecutionTime[j][e.getKey()] + readyTime[e.getKey()];
				}
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	/**
	 * Process the return of a request for the characteristics of a Datacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

		Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloud Resource List received with ",
				getDatacenterIdsList().size(), " resource(s)");

		for (Integer datacenterId : getDatacenterIdsList()) {
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
		}
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": VM #", vmId,
					" has been created in Datacenter #", datacenterId, ", Host #",
					VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
		} else {
			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Creation of VM #", vmId,
					" failed in Datacenter #", datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloudlet ", cloudlet.getCloudletId(),
				" received");
		cloudletsSubmitted--;
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				// all the cloudlets sent finished. It means that some bount
				// cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}

		}
	}

	/**
	 * Process non-default received events that aren't processed by
         * the {@link #processEvent(org.cloudbus.cloudsim.core.SimEvent)} method.
         * This method should be overridden by subclasses in other to process
         * new defined events.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
         * @todo to ensure the method will be overridden, it should be defined 
         * as abstract in a super class from where new brokers have to be extended.
	 */
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
			return;
		}

		Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
	}

	/**
	 * Create the submitted virtual machines in a datacenter.
	 * 
	 * @param datacenterId Id of the chosen Datacenter
	 * @pre $none
	 * @post $none
         * @see #submitVmList(java.util.List) 
	 */
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the next one
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);
		for (Vm vm : getVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);

		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
         * @see #submitCloudletList(java.util.List) 
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
		for (Cloudlet cloudlet : getCloudletList()) {
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
			} else { // submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					if(!Log.isDisabled()) {
					    Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ",
							cloudlet.getCloudletId(), ": bount VM not available");
					}
					continue;
				}
			}

			if (!Log.isDisabled()) {
			    Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
					cloudlet.getCloudletId(), " to VM #", vm.getId());
			}

			cloudlet.setVmId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
			successfullySubmitted.add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		getCloudletList().removeAll(successfullySubmitted);


	}

	/**
	 * Destroy all virtual machines running in datacenters.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printConcatLine(CloudSim.clock(), ": " + getName(), ": Destroying VM #", vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	/**
	 * Send an internal event communicating the end of the simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void finishExecution() {
		sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
	}

	@Override
	public void shutdownEntity() {
		Log.printConcatLine(getName(), " is shutting down...");
	}

	@Override
	public void startEntity() {
		Log.printConcatLine(getName(), " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		return (List<T>) vmList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}

	/**
	 * Gets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletList() {
		return (List<T>) cloudletList;
	}

	/**
	 * Sets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletList the new cloudlet list
	 */
	protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
		this.cloudletList = cloudletList;
	}

	/**
	 * Gets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
		return (List<T>) cloudletSubmittedList;
	}

	/**
	 * Sets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletSubmittedList the new cloudlet submitted list
	 */
	protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
		this.cloudletSubmittedList = cloudletSubmittedList;
	}

	/**
	 * Gets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletReceivedList() {
		return (List<T>) cloudletReceivedList;
	}

	/**
	 * Sets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletReceivedList the new cloudlet received list
	 */
	protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
		this.cloudletReceivedList = cloudletReceivedList;
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmsCreatedList() {
		return (List<T>) vmsCreatedList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmsCreatedList the vms created list
	 */
	protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
		this.vmsCreatedList = vmsCreatedList;
	}

	/**
	 * Gets the vms requested.
	 * 
	 * @return the vms requested
	 */
	protected int getVmsRequested() {
		return vmsRequested;
	}

	/**
	 * Sets the vms requested.
	 * 
	 * @param vmsRequested the new vms requested
	 */
	protected void setVmsRequested(int vmsRequested) {
		this.vmsRequested = vmsRequested;
	}

	/**
	 * Gets the vms acks.
	 * 
	 * @return the vms acks
	 */
	protected int getVmsAcks() {
		return vmsAcks;
	}

	/**
	 * Sets the vms acks.
	 * 
	 * @param vmsAcks the new vms acks
	 */
	protected void setVmsAcks(int vmsAcks) {
		this.vmsAcks = vmsAcks;
	}

	/**
	 * Increment the number of acknowledges (ACKs) sent in response
         * to requests of VM creation.
	 */
	protected void incrementVmsAcks() {
		vmsAcks++;
	}

	/**
	 * Gets the vms destroyed.
	 * 
	 * @return the vms destroyed
	 */
	protected int getVmsDestroyed() {
		return vmsDestroyed;
	}

	/**
	 * Sets the vms destroyed.
	 * 
	 * @param vmsDestroyed the new vms destroyed
	 */
	protected void setVmsDestroyed(int vmsDestroyed) {
		this.vmsDestroyed = vmsDestroyed;
	}

	/**
	 * Gets the datacenter ids list.
	 * 
	 * @return the datacenter ids list
	 */
	protected List<Integer> getDatacenterIdsList() {
		return datacenterIdsList;
	}

	/**
	 * Sets the datacenter ids list.
	 * 
	 * @param datacenterIdsList the new datacenter ids list
	 */
	protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
		this.datacenterIdsList = datacenterIdsList;
	}

	/**
	 * Gets the vms to datacenters map.
	 * 
	 * @return the vms to datacenters map
	 */
	protected Map<Integer, Integer> getVmsToDatacentersMap() {
		return vmsToDatacentersMap;
	}

	/**
	 * Sets the vms to datacenters map.
	 * 
	 * @param vmsToDatacentersMap the vms to datacenters map
	 */
	protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
		this.vmsToDatacentersMap = vmsToDatacentersMap;
	}

	/**
	 * Gets the datacenter characteristics list.
	 * 
	 * @return the datacenter characteristics list
	 */
	protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
		return datacenterCharacteristicsList;
	}

	/**
	 * Sets the datacenter characteristics list.
	 * 
	 * @param datacenterCharacteristicsList the datacenter characteristics list
	 */
	protected void setDatacenterCharacteristicsList(
			Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
		this.datacenterCharacteristicsList = datacenterCharacteristicsList;
	}

	/**
	 * Gets the datacenter requested ids list.
	 * 
	 * @return the datacenter requested ids list
	 */
	protected List<Integer> getDatacenterRequestedIdsList() {
		return datacenterRequestedIdsList;
	}

	/**
	 * Sets the datacenter requested ids list.
	 * 
	 * @param datacenterRequestedIdsList the new datacenter requested ids list
	 */
	protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
		this.datacenterRequestedIdsList = datacenterRequestedIdsList;
	}

}
