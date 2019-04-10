package com.ltf.strategy;

public class ConvertStragegy {

    private ConvertStragegy() {
    }

    public static Object covertType(Class<?> type,String o) {
        return Type.getCovert(type).covertType(o);
    }

    private enum Type{
        /** Integer */
        INTEGER(Integer.class,new CovertInteger()),
        /** Double */
        DOUBLE(Double.class,new CovertDouble()),
        /** Double */
        STRING(String.class,new CovertString()),
        /** Boolean */
        BOOLEAN(Boolean.class,new CovertBoolean());

        Type(Class type, Convert convert) {
            this.type = type;
            this.convert = convert;
        }

        private Class type;
        private Convert convert;

        public static Convert getCovert(Class type) {
            for (Type value : Type.values()) {
                if (value.type == type) {
                    return value.convert;
                }
            }
            return null;
        }

    }
}
