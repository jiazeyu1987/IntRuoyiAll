package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.server.controller.admin.profileworkbench.ProfileWorkbenchTodoController;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.pattern.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.*;
/** Fixed workbench privacy metadata precedes access logging/authentication; never invokes an endpoint. */
public final class WorkbenchRequestLogMetadataFilter extends OncePerRequestFilter {
 private record Endpoint(PathPattern pattern,Method method){}
 private final List<Endpoint> endpoints;
 private final ObjectProvider<ProfileWorkbenchTodoController> controller;
 public WorkbenchRequestLogMetadataFilter(WebProperties properties,ObjectProvider<ProfileWorkbenchTodoController> controller){
  this.controller=controller;var api=properties.getAdminApi();Class<?> type=ProfileWorkbenchTodoController.class;
  Assert.notNull(api,"Workbench admin API configuration missing");Assert.hasText(api.getPrefix(),"Workbench admin prefix missing");Assert.hasText(api.getController(),"Workbench controller pattern missing");
  Assert.state(AnnotatedElementUtils.hasAnnotation(type,RestController.class)&&new AntPathMatcher(".").match(api.getController(),type.getPackageName()),"Workbench controller must receive admin prefix");
  Assert.state(api.getPrefix().startsWith("/")&&!api.getPrefix().endsWith("/"),"Workbench admin prefix must be absolute");
  RequestMapping root=AnnotatedElementUtils.findMergedAnnotation(type,RequestMapping.class);Assert.notNull(root,"Workbench root mapping missing");String rootPath=single(root.path());
  List<Endpoint> result=new ArrayList<>();Set<String> paths=new HashSet<>();
  for(String name:List.of("context","page","count","visibility")){
   var methods=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals(name)).toList();Assert.state(methods.size()==1,"Workbench privacy method missing or ambiguous");Method method=methods.get(0);
   RequestMapping action=AnnotatedElementUtils.findMergedAnnotation(method,RequestMapping.class);ApiAccessLog privacy=AnnotatedElementUtils.findMergedAnnotation(method,ApiAccessLog.class);
   Assert.state(action!=null&&privacy!=null&&privacy.enable()&&!privacy.requestEnable()&&!privacy.responseEnable(),"Workbench privacy must retain audit without payloads");
   var pattern=new PathPatternParser().parse(api.getPrefix()+rootPath+single(action.path()));Assert.state(!pattern.hasPatternSyntax()&&paths.add(pattern.getPatternString()),"Workbench privacy requires distinct fixed paths");result.add(new Endpoint(pattern,method));
  }
  endpoints=List.copyOf(result);
 }
 private static String single(String[] paths){Assert.state(paths.length==1&&paths[0].startsWith("/"),"Workbench requires one fixed mapping");return paths[0];}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var path=RequestPath.parse(request.getRequestURI(),request.getContextPath());
  for(Endpoint endpoint:endpoints)if(endpoint.pattern.matches(path.pathWithinApplication())){
   request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI,request.getContextPath()+endpoint.pattern.getPatternString());
   request.setAttribute(ATTRIBUTE_HANDLER_METHOD,new HandlerMethod(controller.getObject(),endpoint.method));break;
  }
  chain.doFilter(request,response);
 }
}
