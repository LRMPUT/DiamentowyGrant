package org.dg.openAIL;


public class IdPair<A, B> {
    private A first;
    private B second;

    public IdPair(A first, B second) {
   
    	this.first = first;
    	this.second = second;
    }
    public IdPair(IdPair<A, B> tmp) {
 	   
    	this.first = tmp.first;
    	this.second = tmp.second;
    }
    public A getFirst() {
    	return first;
    }

    public void setFirst(A first) {
    	this.first = first;
    }

    public B getSecond() {
    	return second;
    }

    public void setSecond(B second) {
    	this.second = second;
    }
} 