package cn.iocoder.yudao.module.dcc.service.file;
/** Server-created context; no controller flag or client supplied intent participates. */
record DccNameReservationContext(Intent intent,Long selectedControlledFileId) {
    enum Intent { NEW_LOGICAL_FILE, EXISTING_MASTER_VERSION }
    static DccNameReservationContext newLogicalFile(){return new DccNameReservationContext(Intent.NEW_LOGICAL_FILE,null);}
    static DccNameReservationContext existingVersion(Long id){
        if(id==null)throw new IllegalArgumentException("actual selected version required");
        return new DccNameReservationContext(Intent.EXISTING_MASTER_VERSION,id);
    }
}
