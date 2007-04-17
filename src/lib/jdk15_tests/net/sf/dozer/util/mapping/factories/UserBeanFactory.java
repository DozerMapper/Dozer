/*
 * Copyright 2005-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.dozer.util.mapping.factories;

import net.sf.dozer.util.mapping.BeanFactoryIF;
import net.sf.dozer.util.mapping.vo.interfacerecursion.User;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserGroup;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserGroupImpl;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserGroupPrime;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserGroupPrimeImpl;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserImpl;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserPrime;
import net.sf.dozer.util.mapping.vo.interfacerecursion.UserPrimeImpl;

/**
 * @author Christoph Goldner 
 */
public class UserBeanFactory implements BeanFactoryIF {

    public Object createBean(Object aSrcObj, Class aSrcObjClass, String aTargetBeanId) throws RuntimeException {
        
        // check null arguments
        if (aSrcObj == null || aSrcObjClass == null || aTargetBeanId == null) {
            return null;
        }
        
        // get class for target bean name
        Class targetClass;
		try {
			targetClass = Class.forName(aTargetBeanId);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

        // handle known interfaces
        if (User.class.isAssignableFrom(targetClass)) {
        	return new UserImpl();
        }
        if (UserGroup.class.isAssignableFrom(targetClass)) {
        	return new UserGroupImpl();
        }
        if (UserPrime.class.isAssignableFrom(targetClass)) {
        	return new UserPrimeImpl();
        }
        if (UserGroupPrime.class.isAssignableFrom(targetClass)) {
        	return new UserGroupPrimeImpl();
        }
        
        // otherwise create instance directly
        try {
			return targetClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
    }
}
