package jbse.algo.meta;

import static jbse.algo.Util.ensureInstance_JAVA_CLASS;
import static jbse.algo.Util.exitFromAlgorithm;
import static jbse.algo.Util.failExecution;
import static jbse.algo.Util.throwNew;
import static jbse.algo.Util.throwVerifyError;
import static jbse.bc.Signatures.JAVA_OBJECT;
import static jbse.bc.Signatures.OUT_OF_MEMORY_ERROR;
import static jbse.common.Type.ARRAYOF;
import static jbse.common.Type.REFERENCE;
import static jbse.common.Type.TYPEEND;

import java.util.function.Supplier;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.exc.CannotManageStateException;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.bc.exc.BadClassFileException;
import jbse.bc.exc.ClassFileNotAccessibleException;
import jbse.common.exc.ClasspathException;
import jbse.dec.exc.DecisionException;
import jbse.mem.Array;
import jbse.mem.Instance_JAVA_CLASS;
import jbse.mem.State;
import jbse.mem.exc.FastArrayAccessNotAllowedException;
import jbse.mem.exc.HeapMemoryExhaustedException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.Null;
import jbse.val.Reference;
import jbse.val.ReferenceConcrete;
import jbse.val.Value;
import jbse.val.exc.InvalidOperandException;
import jbse.val.exc.InvalidTypeException;

/**
 * Meta-level implementation of {@link java.lang.Class#getEnclosingMethod0()}.
 * 
 * @author Pietro Braione
 */
public final class Algo_JAVA_CLASS_GETENCLOSINGMETHOD0 extends Algo_INVOKEMETA_Nonbranching {
    private Value toPush; //set by cookMore
    
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 1;
    }

    @Override
    protected void cookMore(State state)
    throws ThreadStackEmptyException, DecisionException, ClasspathException,
    CannotManageStateException, InterruptException {
        try {           
            //gets the classfile represented by the "this" parameter
            final Reference classRef = (Reference) this.data.operand(0);
            final Instance_JAVA_CLASS clazz = (Instance_JAVA_CLASS) state.getObject(classRef); //TODO check that operand is concrete and not null
            final String className = clazz.representedClass();
            final ClassFile cf = (clazz.isPrimitive() ? 
                                  state.getClassHierarchy().getClassFilePrimitive(className) : 
                                  state.getClassHierarchy().getClassFile(className));
            final Signature sigEnclosing = cf.getEnclosingMethodOrConstructor();
            if (sigEnclosing == null) {
                this.toPush = Null.getInstance();
            } else {
                //ensures the java.lang.Class of the enclosing class
                ensureInstance_JAVA_CLASS(state, sigEnclosing.getClassName(), sigEnclosing.getClassName(), this.ctx); //TODO should check the accessor?
                
                //gets the (possibly null) descriptor and name of the enclosing method
                final Reference refDescriptor, refName;
                if (sigEnclosing.getName() == null || sigEnclosing.getDescriptor() == null) {
                    refDescriptor = refName = Null.getInstance();
                } else {
                    state.ensureStringLiteral(sigEnclosing.getDescriptor());
                    refDescriptor = state.referenceToStringLiteral(sigEnclosing.getDescriptor());
                    state.ensureStringLiteral(sigEnclosing.getName());
                    refName = state.referenceToStringLiteral(sigEnclosing.getName());
                }
                
                //creates the array
                final ReferenceConcrete arrayRef = state.createArray(null, state.getCalculator().valInt(3), "" + ARRAYOF + REFERENCE + JAVA_OBJECT + TYPEEND);
                this.toPush = arrayRef;
                final Array array = (Array) state.getObject(arrayRef);
                array.setFast(state.getCalculator().valInt(0), state.referenceToInstance_JAVA_CLASS(sigEnclosing.getClassName()));
                array.setFast(state.getCalculator().valInt(1), refName);
                array.setFast(state.getCalculator().valInt(2), refDescriptor);
            }
        } catch (HeapMemoryExhaustedException e) {
            throwNew(state, OUT_OF_MEMORY_ERROR);
            exitFromAlgorithm();
        } catch (ClassCastException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        } catch (BadClassFileException | InvalidTypeException | ClassFileNotAccessibleException |  
                 FastArrayAccessNotAllowedException | InvalidOperandException e) {
            //this should never happen
            failExecution(e);
        }
    }

    @Override
    protected void update(State state) throws ThreadStackEmptyException {
        state.pushOperand(this.toPush);
    }
}