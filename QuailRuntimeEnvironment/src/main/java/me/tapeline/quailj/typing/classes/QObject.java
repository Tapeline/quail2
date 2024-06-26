package me.tapeline.quailj.typing.classes;

import me.tapeline.quailj.runtime.Runtime;
import me.tapeline.quailj.runtime.RuntimeStriker;
import me.tapeline.quailj.runtime.Table;
import me.tapeline.quailj.typing.classes.errors.*;

import java.util.*;

public class QObject {

    public static QNull Val() {
        return QNull.globalNull;
    }
    public static QNumber Val(double value) {
        return new QNumber(value);
    }
    public static QBool Val(boolean value) {
        return value? QBool.globalTrue : QBool.globalFalse;
    }
    public static QString Val(String value) {
        return new QString(value);
    }
    public static QList Val(List<QObject> value) {
        return new QList(value);
    }
    public static QDict Val(HashMap<String, QObject> value) {
        return new QDict(value);
    }
    public static final QObject superObject = new QObject(new Table(), "Object", null, true);
    public static QObject nullSafe(QObject object) {
        return object == null? Val() : object;
    }

    protected Table table;
    protected String className;
    protected QObject parent;

    protected boolean isPrototype;
    protected boolean isInheritable = true;

    public QObject(Table table, String className, QObject parent, boolean isPrototype) {
        this.table = table;
        this.className = className;
        this.parent = parent;
        this.isPrototype = isPrototype;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public QObject getParent() {
        return parent;
    }

    public void setParent(QObject superClass) {
        this.parent = superClass;
    }

    public boolean isPrototype() {
        return isPrototype;
    }

    public void setPrototypeFlag(boolean prototype) {
        isPrototype = prototype;
    }

    public boolean isInheritable() {
        return isInheritable;
    }

    public void setInheritableFlag(boolean inheritable) {
        isInheritable = inheritable;
    }

    public QObject getSuperClass() {
        return isPrototype? parent : parent.getSuperClass();
    }

    public QObject getPrototype() {
        return isPrototype? this : parent;
    }

    public QObject value() {
        if (this instanceof ValueCarrier)
            return ((ValueCarrier) this).getValue();
        return this;
    }

    public QObject derive(Runtime runtime) throws RuntimeStriker {
        if (!isPrototype)
            runtime.error(new QDerivationException("Attempt to inherit from non-prototype value", this));
        return new QObject(new Table(), className, this, false);
    }

    public QObject extendAs(Runtime runtime, String className) throws RuntimeStriker {
        if (!isPrototype)
            runtime.error(new QDerivationException("Attempt to inherit from non-prototype value", this));
        return new QObject(new Table(), className, this, true);
    }

    public QObject newObject(Runtime runtime, List<QObject> args, HashMap<String, QObject> kwargs)
            throws RuntimeStriker {
        QObject blank = derive(runtime);
        if (blank.get("_constructor").isFunc())
            blank = blank.callFromThis(runtime, "_constructor", args, kwargs);
        return blank;
    }

    public QObject copy() {
        QObject copy = new QObject(table, className, parent, isPrototype);
        copy.setInheritableFlag(isInheritable);
        return copy;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public QObject clone() {
        QObject copy = copy();
        copy.getTable().clear();
        table.forEach((k, v) -> copy.getTable().put(
                k,
                v.clone(),
                table.getModifiersFor(k)
        ));
        copy.setPrototypeFlag(isPrototype());
        return copy;
    }

    public QObject get(String name) {
        if (name.startsWith("_")) {
            if (name.equals("_className")) return new QString(className);
            else if (name.equals("_superClass")) return getSuperClass();
            else if (name.equals("_objectPrototype")) return getPrototype();
            else if (name.equals("_isPrototype")) return new QBool(isPrototype);
            else if (name.equals("_isInheritable")) return new QBool(isInheritable);
        }
        if (table.containsKey(name))
            return table.get(name);
        else if (parent != null)
            return parent.get(name);
        else
            return new QNull();
    }

    public boolean containsKey(String name) {
        if (name.startsWith("_")) {
            if (name.equals("_className") ||
                name.equals("_superClass") ||
                name.equals("_objectPrototype") ||
                name.equals("_isPrototype") ||
                name.equals("_isInheritable")) return true;
        }
        if (table.containsKey(name))
            return true;
        else if (parent != null)
            return parent.containsKey(name);
        else
            return false;
    }

    public void set(Runtime runtime, String name, QObject value) throws RuntimeStriker {
        table.put(runtime, name, value);
    }

    public void set(String name, QObject value, int[] modifiers) {
        table.put(name, value, modifiers);
    }

    public void set(String name, QObject value) {
        table.put(name, value);
    }

    public QObject getOverridable(Runtime runtime, String name) throws RuntimeStriker {
        if (containsKey("_get"))
            return callFromThis(runtime, "_get", Collections.singletonList(Val(name)), new HashMap<>());
        return get(name);
    }

    public final void setOverridable(Runtime runtime, String name, QObject value) throws RuntimeStriker {
        if (containsKey("_set"))
            callFromThis(runtime, "_set", Arrays.asList(QObject.Val(name), value), new HashMap<>());
        else if (containsKey("_set_" + name))
            callFromThis(runtime, "_set_" + name, Collections.singletonList(value), new HashMap<>());
        else set(runtime, name, value);
    }

    public QObject callFromThis(Runtime runtime, QObject func, List<QObject> args,
                                      HashMap<String, QObject> kwargs) throws RuntimeStriker {
        if (!isPrototype()) {
            args = new ArrayList<>(args);
            args.add(0, this);
        }
        return func.call(runtime, args, kwargs);
    }

    public QObject callFromThis(Runtime runtime, String func, List<QObject> args,
                                      HashMap<String, QObject> kwargs) throws RuntimeStriker {
        return callFromThis(runtime, get(func), args, kwargs);
    }

    public final boolean instanceOf(QObject parent) {
        // If parent == superObject -> true
        if (parent == superObject) return true;

        // if parent == prototype -> true
        // if same class -> true
        if (parent == getPrototype() || parent == this) return true;

        // if super.instanceof parent -> true
        if (getPrototype() != null && getPrototype().getSuperClass() != null && getPrototype().getSuperClass()
                .instanceOf(parent))
            return true;

        return getSuperClass() != null && getSuperClass().instanceOf(parent);

        // -> false
    }

    public final boolean isNum() {
        return this instanceof QNumber;
    }

    public final boolean isBool() {
        return this instanceof QBool;
    }

    public final boolean isNull() {
        return this instanceof QNull;
    }

    public final boolean isStr() {
        return this instanceof QString;
    }

    public final boolean isList() {
        return this instanceof QList;
    }

    public final boolean isDict() {
        return this instanceof QDict;
    }

    public final boolean isFunc() {
        return this instanceof QFunc;
    }

    public final double numValue() {
        if (this instanceof QNumber)
            return ((QNumber) this).getValue();
        return 0;
    }

    public final boolean boolValue() {
        if (this instanceof QBool)
            return ((QBool) this).getValue();
        return false;
    }

    public final boolean isTrue() {
        if (this instanceof QBool)
            return ((QBool) this).getValue();
        return false;
    }

    public final boolean isFalse() {
        if (this instanceof QBool)
            return !((QBool) this).getValue();
        return false;
    }

    public final String strValue() {
        if (this instanceof QString)
            return ((QString) this).getValue();
        return null;
    }

    public final List<QObject> listValue() {
        if (this instanceof QList)
            return ((QList) this).getValues();
        return null;
    }

    public final HashMap<String, QObject> dictValue() {
        if (this instanceof QDict)
            return ((QDict) this).getValues();
        return null;
    }

    // Actions
    // Defaults

    public QObject defaultCall(Runtime runtime, List<QObject> args, HashMap<String, QObject> kwargs)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedUnaryOperationException(this, "call"));
        return Val();
    }

    public QObject defaultSum(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "+", other));
        return Val();
    }

    public QObject defaultSubtract(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "-", other));
        return Val();
    }

    public QObject defaultMultiply(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "*", other));
        return Val();
    }

    public QObject defaultDivide(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "/", other));
        return Val();
    }

    public QObject defaultIntDivide(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "//", other));
        return Val();
    }

    public QObject defaultModulo(Runtime runtime, QObject other)
            throws RuntimeStriker {
        if (other.isDict()) {
            table.putAll(other.dictValue());
            return this;
        }
        runtime.error(new QUnsupportedOperationException(this, "%", other));
        return Val();
    }

    public QObject defaultPower(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "^", other));
        return Val();
    }

    public QObject defaultShiftLeft(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "<<", other));
        return Val();
    }

    public QObject defaultShiftRight(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, ">>", other));
        return Val();
    }

    public QObject defaultEqualsObject(Runtime runtime, QObject other)
            throws RuntimeStriker {
        return Val(this == other);
    }

    public QObject defaultNotEqualsObject(Runtime runtime, QObject other)
            throws RuntimeStriker {
        return Val(this != other);
    }

    public QObject defaultContainsObject(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "in", other));
        return Val();
    }

    public QObject defaultNotContainsObject(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "not in", other));
        return Val();
    }

    public QObject defaultGreater(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, ">", other));
        return Val();
    }

    public QObject defaultGreaterEqual(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, ">=", other));
        return Val();
    }

    public QObject defaultLess(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "<", other));
        return Val();
    }

    public QObject defaultLessEqual(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "<=", other));
        return Val();
    }

    public QObject defaultNot(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedUnaryOperationException(this, "+"));
        return Val();
    }

    public QObject defaultNegate(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedUnaryOperationException(this, "+"));
        return Val();
    }

    public QObject defaultConvertToString(Runtime runtime)
            throws RuntimeStriker {
        return Val(toString());
    }

    public QObject defaultConvertToNumber(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedConversionException(this, "num"));
        return Val();
    }

    public QObject defaultConvertToBool(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedConversionException(this, "bool"));
        return Val();
    }

    public QObject defaultAnd(Runtime runtime, QObject other)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedOperationException(this, "and", other));
        return Val();
    }

    public QObject defaultOr(Runtime runtime, QObject other)
            throws RuntimeStriker {
        if (isNull())
            return other;
        else
            return this;
    }

    public QObject defaultIndex(Runtime runtime, QObject index)
            throws RuntimeStriker {
        return get(index.toString());
    }

    public QObject defaultIndexSet(Runtime runtime, QObject index, QObject value)
            throws RuntimeStriker {
        set(runtime, index.toString(), value);
        return Val();
    }

    public QObject defaultSubscriptStartEnd(Runtime runtime, QObject start, QObject end)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedSubscriptException(this));
        return Val();
    }

    public QObject defaultSubscriptStartEndStep(Runtime runtime, QObject start, QObject end, QObject step)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedSubscriptException(this));
        return Val();
    }

    public QObject defaultIterateStart(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedIterationException(this));
        return Val();
    }

    public QObject defaultIterateNext(Runtime runtime)
            throws RuntimeStriker {
        runtime.error(new QUnsupportedIterationException(this));
        return Val();
    }

    // Action proxies

    public QObject call(Runtime runtime, List<QObject> args, HashMap<String, QObject> kwargs)
            throws RuntimeStriker {
        if (isPrototype()) {
            return newObject(runtime, args, kwargs);
        }
        if (containsKey("_call"))
            return callFromThis(
                    runtime,
                    "_call",
                    Arrays.asList(QObject.Val(args), QObject.Val(kwargs)),
                    new HashMap<>()
            );
        return defaultCall(runtime, args, kwargs);
    }


    public QObject sum(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_add"))
            return callFromThis(
                    runtime,
                    "_add",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultSum(runtime, other);
    }

    public QObject subtract(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_sub"))
            return callFromThis(
                    runtime,
                    "_sub",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultSubtract(runtime, other);
    }

    public QObject multiply(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_mul"))
            return callFromThis(
                    runtime,
                    "_mul",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultMultiply(runtime, other);
    }

    public QObject divide(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_div"))
            return callFromThis(
                    runtime,
                    "_div",
                    Collections.singletonList(other),
                    new HashMap<>()
            );

        return defaultDivide(runtime, other);
    }

    public QObject intDivide(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_intdiv"))
            return callFromThis(
                    runtime,
                    "_intdiv",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultIntDivide(runtime, other);
    }

    public QObject modulo(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_mod"))
            return callFromThis(
                    runtime,
                    "_mod",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultModulo(runtime, other);
    }

    public QObject power(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_pow"))
            return callFromThis(
                    runtime,
                    "_pow",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultPower(runtime, other);
    }

    public QObject shiftLeft(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_shl"))
            return callFromThis(
                    runtime,
                    "_shl",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultShiftLeft(runtime, other);
    }

    public QObject shiftRight(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_shr"))
            return callFromThis(
                    runtime,
                    "_shr",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultShiftRight(runtime, other);
    }

    public QObject equalsObject(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_eq"))
            return callFromThis(
                    runtime,
                    "_eq",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultEqualsObject(runtime, other);
        // wtf was that: return Val(table.getValues().equals(other.table.getValues()));
    }

    public QObject notEqualsObject(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_neq"))
            return callFromThis(
                    runtime,
                    "_neq",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultNotEqualsObject(runtime, other);
        // wtf was that: return Val(!table.getValues().equals(other.table.getValues()));
    }

    public QObject containsObject(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_contains"))
            return callFromThis(
                    runtime,
                    "_contains",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultContainsObject(runtime, other);
    }

    public QObject notContainsObject(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_notcontains"))
            return callFromThis(
                    runtime,
                    "_notcontains",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultNotContainsObject(runtime, other);
    }

    public QObject greater(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_cmpg"))
            return callFromThis(
                    runtime,
                    "_cmpg",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultGreater(runtime, other);
    }

    public QObject greaterEqual(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_cmpge"))
            return callFromThis(
                    runtime,
                    "_cmpge",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultGreaterEqual(runtime, other);
    }

    public QObject less(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_cmpl"))
            return callFromThis(
                    runtime,
                    "_cmpl",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultLess(runtime, other);
    }

    public QObject lessEqual(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_cmple"))
            return callFromThis(
                    runtime,
                    "_cmple",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultLessEqual(runtime, other);
    }

    public QObject not(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_not"))
            return callFromThis(
                    runtime,
                    "_not",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultNot(runtime);
    }

    public QObject negate(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_neg"))
            return callFromThis(
                    runtime,
                    "_neg",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultNegate(runtime);
    }

    public QObject convertToString(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_tostring"))
            return callFromThis(
                    runtime,
                    "_tostring",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultConvertToString(runtime);
    }

    public QObject convertToBool(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_tobool"))
            return callFromThis(
                    runtime,
                    "_tobool",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultConvertToBool(runtime);
    }

    public QObject convertToNumber(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_tonum"))
            return callFromThis(
                    runtime,
                    "_tonum",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultConvertToNumber(runtime);
    }

    public QObject and(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_and"))
            return callFromThis(
                    runtime,
                    "_and",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultAnd(runtime, other);
    }

    public QObject or(Runtime runtime, QObject other) throws RuntimeStriker {
        if (containsKey("_or"))
            return callFromThis(
                    runtime,
                    "_or",
                    Collections.singletonList(other),
                    new HashMap<>()
            );
        return defaultOr(runtime, other);
    }

    public QObject index(Runtime runtime, QObject index) throws RuntimeStriker {
        if (containsKey("_index"))
            return callFromThis(
                    runtime,
                    "_index",
                    Collections.singletonList(index),
                    new HashMap<>()
            );
        return defaultIndex(runtime, index);
    }

    public QObject indexSet(Runtime runtime, QObject index, QObject value) throws RuntimeStriker {
        if (containsKey("_indexSet"))
            return callFromThis(
                    runtime,
                    "_indexSet",
                    Arrays.asList(index, value),
                    new HashMap<>()
            );
        return defaultIndexSet(runtime, index, value);
    }

    public QObject subscriptStartEnd(Runtime runtime, QObject start, QObject end) throws RuntimeStriker {
        if (containsKey("_subscriptStartEnd"))
            return callFromThis(
                    runtime,
                    "_subscriptStartEnd",
                    Arrays.asList(start, end),
                    new HashMap<>()
            );
        return defaultSubscriptStartEnd(runtime, start, end);
    }

    public QObject subscriptStartEndStep(Runtime runtime, QObject start, QObject end, QObject step)
            throws RuntimeStriker {
        if (containsKey("_subscriptStartEndStep"))
            return callFromThis(
                    runtime,
                    "_subscriptStartEndStep",
                    Arrays.asList(start, end, step),
                    new HashMap<>()
            );
        return defaultSubscriptStartEndStep(runtime, start, end, step);
    }

    public QObject iterateStart(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_iterate"))
            return callFromThis(
                    runtime,
                    "_iterate",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultIterateStart(runtime);
    }

    public QObject iterateNext(Runtime runtime) throws RuntimeStriker {
        if (containsKey("_next"))
            return callFromThis(
                    runtime,
                    "_next",
                    new ArrayList<>(),
                    new HashMap<>()
            );
        return defaultIterateNext(runtime);
    }

    @Override
    public String toString() {
        return "<instance of " + className + ">";
    }

}

// public QObject (call|sum|subtract|multiply|divide|intDivide|
// modulo|power|shiftLeft|shiftRight|greater|greaterEqual|less|
// lessEqual|equalsObject|notEqualsObject|not|negate|convertToString|
// convertToBool|convertToNumber|and|or|indexSet|index|subscriptStartEnd|
// subscriptStartEndStep|iterateStart|iterateNext)