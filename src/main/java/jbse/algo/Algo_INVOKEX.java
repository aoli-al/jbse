package jbse.algo;

import static jbse.algo.Util.continueWith;
import static jbse.algo.Util.ensureClassCreatedAndInitialized;
import static jbse.algo.Util.exitFromAlgorithm;
import static jbse.algo.Util.failExecution;
import static jbse.algo.Util.throwNew;
import static jbse.algo.Util.throwVerifyError;
import static jbse.bc.Offsets.offsetInvoke;
import static jbse.bc.Opcodes.OP_INVOKEHANDLE;
import static jbse.bc.Signatures.ABSTRACT_METHOD_ERROR;
import static jbse.bc.Signatures.ILLEGAL_ACCESS_ERROR;
import static jbse.bc.Signatures.INCOMPATIBLE_CLASS_CHANGE_ERROR;
import static jbse.bc.Signatures.NO_CLASS_DEFINITION_FOUND_ERROR;
import static jbse.bc.Signatures.NO_SUCH_METHOD_ERROR;
import static jbse.bc.Signatures.OUT_OF_MEMORY_ERROR;

import java.util.function.Supplier;

import jbse.bc.exc.BadClassFileException;
import jbse.bc.exc.ClassFileIllFormedException;
import jbse.bc.exc.ClassFileNotFoundException;
import jbse.bc.exc.IncompatibleClassFileException;
import jbse.bc.exc.MethodAbstractException;
import jbse.bc.exc.MethodNotAccessibleException;
import jbse.bc.exc.MethodNotFoundException;
import jbse.dec.exc.InvalidInputException;
import jbse.mem.exc.HeapMemoryExhaustedException;
import jbse.tree.DecisionAlternative_NONE;

/**
 * Algorithm for the invoke* bytecodes
 * (invoke[interface/special/static/virtual]).
 *  
 * @author Pietro Braione
 */
final class Algo_INVOKEX extends Algo_INVOKEX_Abstract {
    private final Algo_INVOKEX_Completion algo_INVOKEX_Completion;

    public Algo_INVOKEX(boolean isInterface, boolean isSpecial, boolean isStatic) {
        super(isInterface, isSpecial, isStatic);
        this.algo_INVOKEX_Completion = new Algo_INVOKEX_Completion(isInterface, isSpecial, isStatic);
    }

    @Override
    protected final BytecodeCooker bytecodeCooker() {
        return (state) -> {
            //performs method resolution
            try {
                resolveMethod(state);
            } catch (ClassFileNotFoundException e) {
                //TODO is it ok?
                throwNew(state, NO_CLASS_DEFINITION_FOUND_ERROR);
                exitFromAlgorithm();
            } catch (ClassFileIllFormedException e) {
                //TODO is it ok?
                throwVerifyError(state);
                exitFromAlgorithm();
            } catch (IncompatibleClassFileException e) {
                throwNew(state, INCOMPATIBLE_CLASS_CHANGE_ERROR);
                exitFromAlgorithm();
            } catch (MethodNotFoundException e) {
                throwNew(state, NO_SUCH_METHOD_ERROR);
                exitFromAlgorithm();
            } catch (MethodNotAccessibleException e) {
                throwNew(state, ILLEGAL_ACCESS_ERROR);
                exitFromAlgorithm();
            } catch (BadClassFileException e) {
                //this should never happen since we already caught both its subclasses
                failExecution(e);
            }

            //checks the resolved method; note that more checks 
            //are done later when the frame is pushed
            check(state);

            //creates and initializes the class of the resolved method in the invokestatic case
            if (this.isStatic) { 
                try {
                    ensureClassCreatedAndInitialized(state, this.methodSignatureResolved.getClassName(), this.ctx);
                } catch (HeapMemoryExhaustedException e) {
                    throwNew(state, OUT_OF_MEMORY_ERROR);
                    exitFromAlgorithm();
                } catch (InvalidInputException | BadClassFileException e) {
                    //this should never happen after resolution 
                    failExecution(e);
                }
            }
            
            //looks for the method implementation with standard lookup
            try {
                findImpl(state);
            } catch (MethodNotAccessibleException e) {
                throwNew(state, ILLEGAL_ACCESS_ERROR);
                exitFromAlgorithm();
            } catch (MethodAbstractException e) {
                throwNew(state, ABSTRACT_METHOD_ERROR);
                exitFromAlgorithm();
            } catch (IncompatibleClassFileException e) {
                throwNew(state, INCOMPATIBLE_CLASS_CHANGE_ERROR);
                exitFromAlgorithm();
            } catch (BadClassFileException e) {
                throwVerifyError(state);
                exitFromAlgorithm();
            }

            //looks for a base-level or meta-level overriding implementation, 
            //and in case considers it instead
            findOverridingImpl(state);

            //if the method has no implementation, raises NoSuchMethodError
            if (this.classFileMethodImpl == null) {
                throwNew(state, NO_SUCH_METHOD_ERROR);
                exitFromAlgorithm();
            }

            //otherwise, concludes the execution of the bytecode algorithm
            if (this.isSignaturePolymorphic) {
                state.getCurrentFrame().patchCode(OP_INVOKEHANDLE);
                exitFromAlgorithm();
            } else {
                this.algo_INVOKEX_Completion.setImplementation(this.classFileMethodImpl, this.methodSignatureImpl);
                this.algo_INVOKEX_Completion.setProgramCounterOffset(returnPcOffset());                
                continueWith(this.algo_INVOKEX_Completion);
            }
        };
    }
    
    /**
     * Override to change the default policy for calculating the PC
     * offset after returning from the invoked method.
     * 
     * @return an {@code int}, the PC offset.
     */
    protected int returnPcOffset() {
        return offsetInvoke(this.isInterface);
    }

    @Override
    protected final Class<DecisionAlternative_NONE> classDecisionAlternative() {
        return null; //never used
    }

    @Override
    protected final StrategyDecide<DecisionAlternative_NONE> decider() {
        return null; //never used
    }

    @Override
    protected final StrategyRefine<DecisionAlternative_NONE> refiner() {
        return null; //never used
    }

    @Override
    protected final StrategyUpdate<DecisionAlternative_NONE> updater() {
        return null; //never used
    }

    @Override
    protected final Supplier<Boolean> isProgramCounterUpdateAnOffset() {
        return null; //never used
    }

    @Override
    protected final Supplier<Integer> programCounterUpdate() {
        return null; //never used
    }
}
