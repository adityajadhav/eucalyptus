/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.images.util;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.util.EntityWrapper;

import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.ws.ImageManager;

public class ImageUtil {
  private static Logger LOG = Logger.getLogger( ImageUtil.class );
  public static String generateImageId( final String imagePrefix, final String imageLocation ) {
    Adler32 hash = new Adler32();
    String key = imageLocation + System.currentTimeMillis();
    hash.update( key.getBytes() );
    String imageId = String.format( "%s-%08X", imagePrefix, hash.getValue() );
    return imageId;
  }

  public static String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo query = new ImageInfo();
    query.setImageId( generateImageId( imagePrefix, imageLocation ) );
    LOG.info( "Trying to lookup using created AMI id=" + query.getImageId() );
    for ( ; db.query( query ).size() != 0; query.setImageId( generateImageId( imagePrefix, imageLocation ) ) ) ;
    db.commit();
    LOG.info( "Assigning imageId=" + query.getImageId() );
    return query.getImageId();
  }

  public static boolean verifyManifestSignature( final String signature, final String alias, String pad ) {
    boolean ret = false;
    try {
      Signature sigVerifier = Signature.getInstance( "SHA1withRSA" );
      X509Certificate cert = CredentialProvider.getCertificate( alias );
      PublicKey publicKey = cert.getPublicKey();
      sigVerifier.initVerify( publicKey );
      sigVerifier.update( pad.getBytes() );
      sigVerifier.verify( Hashes.hexToBytes( signature ) );
      ret = true;
    } catch ( Exception ex ) {
      LOG.warn( ex.getMessage() );
    }
    return ret;
  }

}
