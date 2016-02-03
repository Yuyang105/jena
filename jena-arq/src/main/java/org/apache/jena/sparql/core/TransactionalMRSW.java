/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.sparql.core;

import java.util.Objects ;

import org.apache.jena.query.ReadWrite ;
import org.apache.jena.shared.Lock ;
import org.apache.jena.shared.LockMRSW ;
import org.apache.jena.sparql.JenaTransactionException ;

/** An implementation of Tranactional that provides MRSW locking but no abort.
 *  This is often the best you can do for a system that does not itself provide transactions.
 */ 
public class TransactionalMRSW implements Transactional {
        
    /* To use with implementation inheritance, for when you can't inherit:
        private final Transactional txn = new TransactionalMRSW() ;
        @Override public void begin(ReadWrite mode) { txn.begin(mode) ; }
        @Override public void commit()              { txn.commit() ; }
        @Override public void abort()               { txn.abort() ; }
        @Override public boolean isInTransaction()  { return txn.isInTransaction() ; }
        @Override public void end()                 { txn.end(); }
     */
    
    private final ThreadLocal<ReadWrite> txnMode  = ThreadLocal.withInitial( ()->null ) ;
    private final Lock lock ;

    public TransactionalMRSW(Lock lock) {
        this.lock = lock ;
    }

    public TransactionalMRSW() {
        this(new LockMRSW()) ;
    }

    @Override
    public void begin(ReadWrite readWrite) {
        if ( isInTransaction() )
            error("Already in a transaction") ;
        boolean isRead = readWrite.equals(ReadWrite.READ) ;
        lock.enterCriticalSection(isRead) ;
        txnMode.set(readWrite) ;
    }

    @Override
    public void commit() {
        endOnce() ;
    }

    @Override
    public void abort() {
        if ( isTransactionType(ReadWrite.WRITE) )
            error("Transactional.abort()") ;
        endOnce() ;
    }

    @Override
    public boolean isInTransaction() {
        return txnMode.get() != null ;
    }

    public boolean isTransactionType(ReadWrite mode) {
        return Objects.equals(mode, txnMode.get()) ;
    }

    private ReadWrite getTransactionType(ReadWrite mode) {
        return txnMode.get() ;
    }

    @Override
    public void end() {
        if ( isTransactionType(ReadWrite.WRITE) )
            error("Write tranasction - no commit or abort before end()") ;
        endOnce() ;
    }

    private void endOnce() {
        if ( isInTransaction() ) {
            lock.leaveCriticalSection() ;
            txnMode.remove();
        }
    }
    
    private void error(String msg) {
        throw new JenaTransactionException(msg) ; 
    }
}
