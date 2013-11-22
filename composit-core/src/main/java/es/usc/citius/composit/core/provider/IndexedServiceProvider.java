package es.usc.citius.composit.core.provider;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import es.usc.citius.composit.core.model.Operation;
import es.usc.citius.composit.core.model.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Pablo Rodríguez Mier <<a href="mailto:pablo.rodriguez.mier@usc.es">pablo.rodriguez.mier@usc.es</a>>
 */
public class IndexedServiceProvider<E> implements ServiceDataProvider<E> {
    private Map<String, Service<E>> serviceIndex = new HashMap<String, Service<E>>();
    private Map<String, Operation<E>> operationIndex = new HashMap<String, Operation<E>>();
    private Multimap<E, Operation<E>> inputIndex = HashMultimap.create();
    private Multimap<E, Operation<E>> outputIndex = HashMultimap.create();
    private ServiceDataProvider<E> delegatedProvider;

    public IndexedServiceProvider(ServiceDataProvider<E> delegatedProvider) {
        this.delegatedProvider = delegatedProvider;
        // Build index
        for(Service<E> service : delegatedProvider.getServices()){
            serviceIndex.put(service.getID(), service);
            for(Operation<E> op : service.getOperations()){
                operationIndex.put(op.getID(), op);
                for(E input : op.getSignature().getInputs()){
                    inputIndex.get(input).add(op);
                }
                for(E output : op.getSignature().getOutputs()){
                    outputIndex.get(output).add(op);
                }
            }
        }
    }

    @Override
    public Service<E> getService(String serviceID) {
        return serviceIndex.get(serviceID);
    }

    @Override
    public Operation<E> getOperation(String operationID) {
        return operationIndex.get(operationID);
    }

    @Override
    public Set<Operation<E>> getOperationsWithInput(E input) {
        return ImmutableSet.copyOf(inputIndex.get(input));
    }

    @Override
    public Set<Operation<E>> getOperationsWithOutput(E output) {
        return ImmutableSet.copyOf(outputIndex.get(output));
    }

    @Override
    public Iterable<Operation<E>> getOperations() {
        return ImmutableSet.copyOf(operationIndex.values());
    }

    @Override
    public Iterable<Service<E>> getServices() {
        return ImmutableSet.copyOf(serviceIndex.values());
    }

    @Override
    public Set<String> listOperations() {
        return ImmutableSet.copyOf(operationIndex.keySet());
    }

    @Override
    public Set<String> listServices() {
        return ImmutableSet.copyOf(serviceIndex.keySet());
    }
}