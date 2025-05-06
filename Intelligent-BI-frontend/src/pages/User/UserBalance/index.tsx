import React from 'react';
import {Card, message, Space, Tooltip, Typography} from 'antd';
import {history, useModel} from '@@/exports';
import { Button } from 'antd/lib';
import {userLoginUsingPost} from "@/services/Intelligent BI/userController";
import {doDailyCheckIn1UsingPost, doDailyCheckInUsingPost} from "@/services/Intelligent BI/dailyCheckInController";

const App: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState ?? {};

  const DailyCheck = async (values: API.User) => {
    try {
      // 每日签到
      const res = await doDailyCheckInUsingPost(values);
      if (res.code === 0) {
        // window.location.reload();
        const defaultLoginSuccessMessage = '签到成功！';
        message.success(defaultLoginSuccessMessage);
        setTimeout(() => {
          window.location.reload();
        }, 2000); // 在弹窗关闭后延迟2秒钟刷新页面
        return;
      } else {
        message.error(res.message);
      }
    } catch (error) {
      console.log(error);
    }
  };

  return (
    <Space direction="vertical" size={32}>
      <Card title="我的特权">
        {currentUser && (
          <><p>
            积分: <Typography.Text style={{marginLeft: '10px'}}>{currentUser.balance}</Typography.Text>
          </p><Button type="primary" onClick={DailyCheck}>每日签到</Button></>
        )}
      </Card>
      <Card title="我的信息">
        {currentUser && (
          <p>
            id:
            <Tooltip title="复制">
              <Typography.Text copyable style={{ marginLeft: '10px' }}>{currentUser.id}</Typography.Text>
            </Tooltip>
          </p>
        )}
      </Card>
    </Space>
  );
};

export default App;
