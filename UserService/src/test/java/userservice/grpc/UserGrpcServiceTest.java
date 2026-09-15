package userservice.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import user.contract.v1.*;
import userservice.mapper.command.UserCommandMapper;
import userservice.mapper.grpc.UserGrpcMapper;
import userservice.service.UserService;

class UserGrpcServiceTest {
  private final UserService service = mock(UserService.class);
  private final UserCommandMapper commands = mock(UserCommandMapper.class);
  private final UserGrpcMapper responses = mock(UserGrpcMapper.class);
  private final UserGrpcService grpc = new UserGrpcService(service, commands, responses);

  @Test
  void handlesHealthAndUserQueries() {
    var health = mock(StreamObserver.class);
    grpc.getUserHealth(Empty.getDefaultInstance(), health);
    verify(health).onNext(any(GetUserHealthGrpcResponse.class));
    verify(health).onCompleted();
    var info = mock(StreamObserver.class);
    grpc.getUserInfo(GetUserInfoGrpcRequest.getDefaultInstance(), info);
    verify(info).onCompleted();
    var all = mock(StreamObserver.class);
    grpc.getAllUserInfo(Empty.getDefaultInstance(), all);
    verify(all).onCompleted();
    var recipient = mock(StreamObserver.class);
    grpc.getRecipientByEmail(GetRecipientByEmailRequest.getDefaultInstance(), recipient);
    verify(recipient).onCompleted();
  }
}
