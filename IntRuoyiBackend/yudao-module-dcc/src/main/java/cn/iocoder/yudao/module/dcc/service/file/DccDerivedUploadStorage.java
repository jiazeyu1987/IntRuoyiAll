package cn.iocoder.yudao.module.dcc.service.file;
/** Internal immutable result, never a REST input or a client-supplied directory authority. */
public final class DccDerivedUploadStorage {
 public final Long tenantId,actorId,projectId,folderId,categoryId,mappingId,baseDirectoryId,storageDirectoryId;
 public final boolean created;
 DccDerivedUploadStorage(Long tenant,Long actor,Long project,Long folder,Long category,Long mapping,Long base,Long storage,boolean created) {
  this.tenantId=tenant;this.actorId=actor;this.projectId=project;this.folderId=folder;this.categoryId=category;
  this.mappingId=mapping;this.baseDirectoryId=base;this.storageDirectoryId=storage;this.created=created;
 }
}
